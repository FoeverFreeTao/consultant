package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
import com.zyt.consultant.metrics.BusinessMetrics;
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

import java.util.List;


@RestController
public class ChatController {
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
    @Qualifier("preprocessChatModel")
    private ChatModel preprocessChatModel;
    @Value("${app.llm.preprocess-enabled:false}")
    private boolean preprocessEnabled;

    @RequestMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam("memoryId") String memoryId,
                             @RequestParam("message") String message,
                             @RequestParam(value = "skillIds", required = false) String skillIds) {
        String preprocessedMessage = preprocessEnabled ? preprocessUserMessage(message) : message;
        String enhancedMessage = enrichMessageWithSkills(memoryId, preprocessedMessage, skillIds);
        businessMetrics.recordChatRequest(message);
        long start = System.nanoTime();
        Flux<String> result = consultantService.chat(memoryId, enhancedMessage, memoryId)
                .doOnComplete(() -> businessMetrics.recordChatResult(true, System.nanoTime() - start))
                .doOnError(error -> businessMetrics.recordChatResult(false, System.nanoTime() - start));
        return result;
    }

    private String preprocessUserMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isBlank()) {
            return originalMessage;
        }
        try {
            String prompt = """
                    你是输入预处理助手。请对用户输入做如下处理后输出：
                    1) 保留原始意图，不改变用户目标；
                    2) 修复错别字、口语化歧义；
                    3) 补齐有助于检索的关键词（如餐次、目标、约束）；
                    4) 只输出重写后的单段文本，不要解释。

                    用户输入：
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

//    @RequestMapping("/chat")
//    public String chat(@RequestParam("message") String message){
//        String result = consultantService.chat(message);
//        return result;
//    }
}
