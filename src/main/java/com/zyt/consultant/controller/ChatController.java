package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
import com.zyt.consultant.guardrail.ChatInputGuardrailService;
import com.zyt.consultant.guardrail.InputRiskBlockedException;
import com.zyt.consultant.metrics.BusinessMetrics;
import com.zyt.consultant.rag.ReferenceSourceContext;
import com.zyt.consultant.service.ChatSessionService;
import com.zyt.consultant.service.SkillService;
import com.zyt.consultant.service.impl.SkillServiceImpl;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;


@RestController
public class ChatController {
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
            businessMetrics.recordUserAction("chat_guardrail_block", "blocked");
            businessMetrics.recordChatResult(false, System.nanoTime() - start);
            return Flux.just(buildGuardrailBlockedResponse());
        }
        String preprocessedMessage = preprocessEnabled ? preprocessUserMessage(safeMessage) : safeMessage;
        String enhancedMessage = enrichMessageWithSkills(memoryId, preprocessedMessage, skillIds);
        chatSessionService.touchSessionByMemoryId(memoryId, safeMessage);
        businessMetrics.recordChatRequest(safeMessage);
        ReferenceSourceContext.clear();
        return consultantService.chat(memoryId, enhancedMessage, memoryId)
                .collectList()
                .flatMapMany(chunks -> {
                    String answer = String.join("", chunks).trim();
                    String finalAnswer = appendReferenceSection(answer, ReferenceSourceContext.snapshot());
                    businessMetrics.recordChatResult(true, System.nanoTime() - start);
                    ReferenceSourceContext.clear();
                    return Flux.just(finalAnswer);
                })
                .doOnError(error -> {
                    ReferenceSourceContext.clear();
                    businessMetrics.recordChatResult(false, System.nanoTime() - start);
                });
    }

    private String buildGuardrailBlockedResponse() {
        return "\u5F53\u524D\u8F93\u5165\u88AB\u5B89\u5168\u62E6\u622A\u3002\u8BF7\u907F\u514D\u654F\u611F\u8BCD\u3001\u8D8A\u72F1\u6307\u4EE4\u6216\u63D0\u793A\u6CE8\u5165\u8868\u8FBE\u540E\u518D\u8BD5\u3002";
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


