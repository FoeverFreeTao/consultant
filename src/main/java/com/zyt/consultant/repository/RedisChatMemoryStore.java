package com.zyt.consultant.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${app.llm.context-window-max-chars:12000}")
    private int contextWindowMaxChars;

    @Value("${app.llm.context-window-recent-reserve:4}")
    private int contextWindowRecentReserve;

    @Value("${app.llm.chat-memory-ttl-days:1}")
    private long chatMemoryTtlDays;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = String.valueOf(memoryId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        List<ChatMessage> list = ChatMessageDeserializer.messagesFromJson(json);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String key = String.valueOf(memoryId);
        List<ChatMessage> optimized = applySlidingWindow(list);
        String json = ChatMessageSerializer.messagesToJson(optimized);
        redisTemplate.opsForValue().set(key, json, Duration.ofDays(Math.max(1, chatMemoryTtlDays)));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(memoryId.toString());
    }

    private List<ChatMessage> applySlidingWindow(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        int maxChars = Math.max(1000, contextWindowMaxChars);
        int recentReserve = Math.max(1, contextWindowRecentReserve);

        int totalChars = estimateChars(messages);
        if (totalChars <= maxChars) {
            return messages;
        }

        List<ChatMessage> headSystemMessages = new ArrayList<>();
        int nonSystemStart = 0;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message instanceof SystemMessage) {
                headSystemMessages.add(message);
                nonSystemStart = i + 1;
                continue;
            }
            break;
        }

        List<ChatMessage> nonSystemMessages = messages.subList(nonSystemStart, messages.size());
        Deque<ChatMessage> window = new ArrayDeque<>();
        int usedChars = estimateChars(headSystemMessages);

        for (int i = nonSystemMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = nonSystemMessages.get(i);
            int messageChars = estimateChars(message);
            int distanceFromLatest = (nonSystemMessages.size() - 1) - i;
            boolean mustKeepRecent = distanceFromLatest < recentReserve;

            if (!mustKeepRecent && usedChars + messageChars > maxChars) {
                continue;
            }
            window.addFirst(message);
            usedChars += messageChars;
        }

        List<ChatMessage> result = new ArrayList<>(headSystemMessages.size() + window.size());
        result.addAll(headSystemMessages);
        result.addAll(window);

        if (result.isEmpty()) {
            result.add(messages.get(messages.size() - 1));
        }
        return result;
    }

    private int estimateChars(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return ChatMessageSerializer.messagesToJson(messages).length();
    }

    private int estimateChars(ChatMessage message) {
        return estimateChars(Collections.singletonList(message));
    }
}
