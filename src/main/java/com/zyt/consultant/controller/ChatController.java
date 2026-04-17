package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
import com.zyt.consultant.metrics.BusinessMetrics;
import com.zyt.consultant.service.impl.SkillServiceImpl;
import com.zyt.consultant.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam("memoryId") String memoryId,
                             @RequestParam("message") String message,
                             @RequestParam(value = "skillIds", required = false) String skillIds) {
        String enhancedMessage = enrichMessageWithSkills(memoryId, message, skillIds);
        businessMetrics.recordChatRequest(message);
        long start = System.nanoTime();
        Flux<String> result = consultantService.chat(memoryId, enhancedMessage, memoryId)
                .doOnComplete(() -> businessMetrics.recordChatResult(true, System.nanoTime() - start))
                .doOnError(error -> businessMetrics.recordChatResult(false, System.nanoTime() - start));
        return result;
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
