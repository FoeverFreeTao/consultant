package com.zyt.consultant.controller;

import com.zyt.consultant.aiservice.ConsultantService;
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
    @RequestMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message){
        Flux<String> result = consultantService.chat(memoryId, message, memoryId);
        return result;
    }

//    @RequestMapping("/chat")
//    public String chat(@RequestParam("message") String message){
//        String result = consultantService.chat(message);
//        return result;
//    }
}
