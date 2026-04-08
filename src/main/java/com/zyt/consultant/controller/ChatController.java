package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
import com.zyt.consultant.metrics.BusinessMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


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
    @RequestMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message){
        businessMetrics.recordChatRequest(message);
        long start = System.nanoTime();
        Flux<String> result = consultantService.chat(memoryId, message, memoryId)
                .doOnComplete(() -> businessMetrics.recordChatResult(true, System.nanoTime() - start))
                .doOnError(error -> businessMetrics.recordChatResult(false, System.nanoTime() - start));
        return result;
    }

//    @RequestMapping("/chat")
//    public String chat(@RequestParam("message") String message){
//        String result = consultantService.chat(message);
//        return result;
//    }
}
