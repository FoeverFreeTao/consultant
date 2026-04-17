package com.zyt.consultant.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemory = "chatMemory",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",
        tools = {
                "nutritionAnalysisTool",
                "dietRecommendationTool",
                "dailyStatusHealthTool"
        }
)
public interface ConsultantService {

    @SystemMessage(fromResource = "system.txt")
    @UserMessage("""
            {{message}}

            如果使用了知识库检索内容，请在回答最后增加“参考来源”小节，
            每行使用 [来源:xxx] 的格式列出你实际参考到的来源标签。
            """)
    Flux<String> chat(
            @MemoryId String memoryId,
            @V("message") String message,
            @V("currentMemoryId") String currentMemoryId
    );
}
