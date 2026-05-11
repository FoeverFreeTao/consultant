package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
import com.zyt.consultant.guardrail.ChatInputGuardrailService;
import com.zyt.consultant.guardrail.InputRiskBlockedException;
import com.zyt.consultant.metrics.BusinessMetrics;
import com.zyt.consultant.rag.ReferenceSourceContext;
import com.zyt.consultant.service.ChatSessionService;
import com.zyt.consultant.service.SkillService;
import com.zyt.consultant.service.VisionMealAnalysisService;
import com.zyt.consultant.service.impl.SkillServiceImpl;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;


@RestController
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final List<String> INTERNAL_PROMPT_PREFIXES = List.of(
            "Use retrieved knowledge only as background evidence.",
            "Summarize the meaning in your own words.",
            "Never expose raw retrieved snippets",
            "Answer using the following information:",
            "Internal reference material.",
            "If the user describes what they ate or drank, asks for calories, nutrition, protein, fat, carbs, or says things like",
            "For Chinese natural-language diet descriptions, prefer passing only the cleaned food list into nutritionAnalysisTool",
            "Never estimate food calories yourself when nutritionAnalysisTool should be used."
    );

//    @Autowired
//    private OpenAiChatModel model;
//    @RequestMapping("/chat")
//    public String chat(@RequestParam("message")String message){
//        String result = model.chat(message);
//        return result;
//    }
    @Autowired
    private ConsultantService consultantService;
    @Autowired
    private BusinessMetrics businessMetrics;
    @Autowired
    private SkillService skillService;
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private ChatInputGuardrailService chatInputGuardrailService;
    @Autowired
    private VisionMealAnalysisService visionMealAnalysisService;
    @Autowired
    @Qualifier("preprocessChatModel")
    private ChatModel preprocessChatModel;
    @Value("${app.llm.preprocess-enabled:false}")
    private boolean preprocessEnabled;

    @RequestMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam("memoryId") String memoryId,
                             @RequestParam("message") String message,
                             @RequestParam(value = "skillIds", required = false) String skillIds) {
        long start = System.nanoTime();
        String safeMessage;
        try {
            safeMessage = chatInputGuardrailService.validate(message);
        } catch (InputRiskBlockedException ex) {
            safeRecordUserAction("chat_guardrail_block", "blocked");
            safeRecordChatResult(false, System.nanoTime() - start);
            return Flux.just(buildGuardrailBlockedResponse());
        } catch (Exception ex) {
            log.warn("chat guardrail unavailable, continuing with original message", ex);
            safeMessage = message;
        }
        final String chatMessage = safeMessage;
        String preprocessedMessage = preprocessEnabled ? preprocessUserMessage(chatMessage) : chatMessage;
        String enhancedMessage = safeEnrichMessageWithSkills(memoryId, preprocessedMessage, skillIds);
        safeTouchSession(memoryId, chatMessage);
        safeRecordChatRequest(chatMessage);
        ReferenceSourceContext.clear();

        Flux<String> responseFlux;
        try {
            responseFlux = consultantService.chat(memoryId, enhancedMessage, memoryId);
        } catch (Exception ex) {
            return buildFallbackFlux(chatMessage, start, ex);
        }
        if (responseFlux == null) {
            return buildFallbackFlux(chatMessage, start, new IllegalStateException("consultant service returned null flux"));
        }

        return responseFlux
                .collectList()
                .flatMapMany(chunks -> {
                    String answer = String.join("", chunks).trim();
                    if (answer.isBlank()) {
                        safeRecordChatResult(false, System.nanoTime() - start);
                        return Flux.just(buildBackendFallbackResponse(chatMessage));
                    }
                    String finalAnswer = appendReferenceSection(answer, ReferenceSourceContext.snapshot());
                    safeRecordChatResult(true, System.nanoTime() - start);
                    return Flux.just(finalAnswer);
                })
                .onErrorResume(error -> buildFallbackFlux(chatMessage, start, error))
                .doFinally(signalType -> {
                    ReferenceSourceContext.clear();
                });
    }

    @PostMapping(value = "/chat/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String chatWithMealImage(@RequestParam("memoryId") String memoryId,
                                    @RequestParam(value = "message", required = false) String message,
                                    @RequestParam("image") MultipartFile image) {
        long start = System.nanoTime();
        String userMessage = message == null ? "" : message.trim();
        if (userMessage.isBlank()) {
            userMessage = "请识别这张餐食照片，并估算食物份量和营养价值。";
        }
        try {
            chatInputGuardrailService.validate(userMessage);
        } catch (InputRiskBlockedException ex) {
            safeRecordUserAction("chat_image_guardrail_block", "blocked");
            safeRecordChatResult(false, System.nanoTime() - start);
            return buildGuardrailBlockedResponse();
        } catch (Exception ex) {
            log.warn("image chat guardrail unavailable, continuing with original message", ex);
        }

        String userContent = "[餐食照片] " + userMessage;
        safeTouchSession(memoryId, userContent);
        safeRecordChatRequest(userContent);

        try {
            String answer = visionMealAnalysisService.analyzeMealImage(image, userMessage);
            safeAppendExchange(memoryId, userContent, answer);
            safeRecordChatResult(true, System.nanoTime() - start);
            return answer;
        } catch (Exception ex) {
            log.warn("image chat fallback triggered", ex);
            safeRecordChatResult(false, System.nanoTime() - start);
            return "我暂时无法完成餐食图片识别。你可以先用文字告诉我图片里的食物和大致份量，我会继续帮你估算热量、蛋白质、脂肪和碳水。";
        }
    }

    private String buildGuardrailBlockedResponse() {
        return "\u5F53\u524D\u8F93\u5165\u88AB\u5B89\u5168\u62E6\u622A\u3002\u8BF7\u907F\u514D\u654F\u611F\u8BCD\u3001\u8D8A\u72F1\u6307\u4EE4\u6216\u63D0\u793A\u6CE8\u5165\u8868\u8FBE\u540E\u518D\u8BD5\u3002";
    }

    private Flux<String> buildFallbackFlux(String safeMessage, long start, Throwable error) {
        log.warn("chat backend fallback triggered", error);
        ReferenceSourceContext.clear();
        safeRecordChatResult(false, System.nanoTime() - start);
        return Flux.just(buildBackendFallbackResponse(safeMessage));
    }

    private String buildBackendFallbackResponse(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase();
        if (containsAny(normalized, "热量", "卡路里", "多少卡", "kcal", "蛋白质", "脂肪", "碳水", "吃了", "喝了")) {
            return "我暂时无法调用营养分析工具，不能给出精确热量和营养数值。可以先按这个方向处理：尽量补充每种食物的克重或份量，主食、蛋白质和蔬菜分开记录；如果是减脂餐，优先选择少油烹饪、足量优质蛋白和高纤维蔬菜。稍后工具恢复后，我可以再帮你做精确计算。";
        }
        if (containsAny(normalized, "饮水", "喝水", "睡眠", "睡了", "运动", "日常状态", "健康状态")) {
            return "我暂时无法读取或调用日常健康状态工具。你可以先参考通用目标：饮水约 1500-2000ml，睡眠 7-9 小时，每天至少 30 分钟中等强度活动；如果有不适或基础疾病，请以医生建议为准。";
        }
        if (containsAny(normalized, "bmi", "饮食推荐", "减脂", "增肌", "偏瘦", "肥胖")) {
            return "我暂时无法调用饮食推荐工具。可以先用通用原则兜底：三餐保持规律，每餐包含主食、优质蛋白和蔬菜；减脂时减少油炸和含糖饮料，增肌或偏瘦时适量增加蛋白质和总能量摄入。";
        }
        return "我这边暂时无法完成后端工具调用，但已经进入兜底模式。你可以先把目标、当前情况和关键数据说清楚，我会基于通用健康饮食原则给出稳妥建议；需要精确计算或读取个人数据时，等工具恢复后再继续处理。";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String preprocessUserMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isBlank()) {
            return originalMessage;
        }
        try {
            String prompt = """
                    你是一个用户输入预处理助手，请在不改变原始意图的前提下，对下面这段用户输入做轻量整理。

                    要求：
                    1. 保留用户原本想问的问题，不要扩写，不要回答问题。
                    2. 删除明显多余的口头语、重复表达和无意义噪声。
                    3. 保留关键实体、食物名称、数量、时间、症状、目标等关键信息。
                    4. 如果用户在描述吃了什么，可以整理语序，但不要删除任何食物名称。
                    5. 输出只保留整理后的最终文本，不要加解释、标题、前后缀。

                    用户原始输入：
                    %s
                    """.formatted(originalMessage);
            String rewritten = preprocessChatModel.chat(prompt);
            if (rewritten == null || rewritten.isBlank()) {
                return originalMessage;
            }
            return rewritten.trim();
        } catch (Exception ex) {
            return originalMessage;
        }
    }
    private String enrichMessageWithSkills(String memoryId, String message, String skillIds) {
        String skillPrompt;
        if (skillIds != null && !skillIds.trim().isEmpty()) {
            List<String> ids = SkillServiceImpl.parseSkillIds(skillIds);
            skillPrompt = skillService.buildSkillPromptBySkillIds(ids);
        } else {
            skillPrompt = skillService.buildSkillPromptByMemoryId(memoryId);
        }
        if (skillPrompt == null || skillPrompt.isBlank()) {
            return message;
        }
        return skillPrompt + "\n\nUser question:\n" + message;
    }

    private String safeEnrichMessageWithSkills(String memoryId, String message, String skillIds) {
        try {
            return enrichMessageWithSkills(memoryId, message, skillIds);
        } catch (Exception ex) {
            log.warn("skill prompt fallback, memoryId={}", memoryId, ex);
            return message;
        }
    }

    private void safeTouchSession(String memoryId, String message) {
        try {
            chatSessionService.touchSessionByMemoryId(memoryId, message);
        } catch (Exception ex) {
            log.warn("chat session touch failed, memoryId={}", memoryId, ex);
        }
    }

    private void safeRecordChatRequest(String message) {
        try {
            businessMetrics.recordChatRequest(message);
        } catch (Exception ex) {
            log.warn("record chat request metrics failed", ex);
        }
    }

    private void safeRecordChatResult(boolean success, long nanos) {
        try {
            businessMetrics.recordChatResult(success, nanos);
        } catch (Exception ex) {
            log.warn("record chat result metrics failed", ex);
        }
    }

    private void safeRecordUserAction(String action, String status) {
        try {
            businessMetrics.recordUserAction(action, status);
        } catch (Exception ex) {
            log.warn("record user action metrics failed, action={}", action, ex);
        }
    }

    private void safeAppendExchange(String memoryId, String userContent, String assistantContent) {
        try {
            chatSessionService.appendExchangeByMemoryId(memoryId, userContent, assistantContent);
        } catch (Exception ex) {
            log.warn("append image chat exchange failed, memoryId={}", memoryId, ex);
        }
    }

    private String appendReferenceSection(String answer, List<String> rawSources) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        String sanitizedAnswer = sanitizeAnswerContent(answer);
        List<String> sources = deduplicateSources(rawSources);
        String cleanedAnswer = stripReferenceSection(sanitizedAnswer);
        if (sources.isEmpty()) {
            return cleanedAnswer;
        }
        StringBuilder sb = new StringBuilder(cleanedAnswer.trim());
        sb.append("\n\n\u53C2\u8003\u6765\u6E90\n");
        for (String source : sources) {
            sb.append("[\u6765\u6E90:").append(source).append("]\n");
        }
        return sb.toString().trim();
    }

    private List<String> deduplicateSources(List<String> rawSources) {
        List<String> result = new ArrayList<>();
        if (rawSources == null || rawSources.isEmpty()) {
            return result;
        }
        for (String source : rawSources) {
            if (source == null) {
                continue;
            }
            String cleaned = source.trim();
            if (cleaned.isEmpty() || result.contains(cleaned)) {
                continue;
            }
            result.add(cleaned);
        }
        return result;
    }

    private String sanitizeAnswerContent(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        String[] lines = answer.replace("\r\n", "\n").split("\n");
        StringBuilder cleaned = new StringBuilder();
        boolean skipSnippetLine = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                skipSnippetLine = false;
                if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) != '\n') {
                    cleaned.append('\n');
                }
                continue;
            }
            if (isInternalPromptLine(trimmed)) {
                skipSnippetLine = false;
                continue;
            }
            if (trimmed.startsWith("[Source:")) {
                skipSnippetLine = true;
                continue;
            }
            if (skipSnippetLine) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString().trim();
    }

    private boolean isInternalPromptLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        for (String prefix : INTERNAL_PROMPT_PREFIXES) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String stripReferenceSection(String answer) {
        String trimmed = answer == null ? "" : answer.trim();
        int index = trimmed.indexOf("\n\u53C2\u8003\u6765\u6E90");
        if (index >= 0) {
            return trimmed.substring(0, index).trim();
        }
        index = trimmed.indexOf("\n## \u53C2\u8003\u6765\u6E90");
        if (index >= 0) {
            return trimmed.substring(0, index).trim();
        }
        return trimmed;
    }

//    @RequestMapping("/chat")
//    public String chat(@RequestParam("message") String message){
//        String result = consultantService.chat(message);
//        return result;
//    }
}


