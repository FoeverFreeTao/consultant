package com.zyt.consultant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.consultant.repository.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatSessionService {

    private static final Pattern MEMORY_KEY_PATTERN = Pattern.compile("^chat:memory:user:(\\d+):session:([\\w-]+)$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String DEFAULT_TITLE = "New Chat";
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

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisChatMemoryStore chatMemoryStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.llm.chat-memory-ttl-days:1}")
    private long chatMemoryTtlDays;

    public List<ChatSessionMeta> listSessions(Long userId) {
        Set<String> orderedIds = redisTemplate.opsForZSet().reverseRange(buildSessionOrderKey(userId), 0, -1);
        if (orderedIds == null || orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatSessionMeta> sessions = new ArrayList<>();
        boolean keptEmptyDefaultSession = false;
        for (String sessionId : orderedIds) {
            ChatSessionMeta meta = getSessionMeta(userId, sessionId);
            if (meta != null) {
                if (isEmptyDefaultSession(userId, meta)) {
                    if (keptEmptyDefaultSession) {
                        deleteSession(userId, meta.getSessionId());
                        continue;
                    }
                    keptEmptyDefaultSession = true;
                }
                sessions.add(meta);
            }
        }
        sessions.sort(Comparator.comparingLong(ChatSessionMeta::getUpdatedAt).reversed());
        refreshTtl(userId, null);
        return sessions;
    }

    public ChatSessionMeta createSession(Long userId, String title) {
        long now = System.currentTimeMillis();
        ChatSessionMeta meta = new ChatSessionMeta();
        meta.setSessionId("session-" + UUID.randomUUID().toString().replace("-", ""));
        meta.setTitle(StringUtils.hasText(title) ? title.trim() : DEFAULT_TITLE);
        meta.setCreatedAt(now);
        meta.setUpdatedAt(now);
        saveSessionMeta(userId, meta);
        return meta;
    }

    public ChatSessionMeta touchSession(Long userId, String sessionId, String titleHint) {
        ChatSessionMeta meta = getSessionMeta(userId, sessionId);
        long now = System.currentTimeMillis();
        if (meta == null) {
            meta = new ChatSessionMeta();
            meta.setSessionId(sessionId);
            meta.setCreatedAt(now);
            meta.setTitle(resolveTitle(titleHint));
        } else if (shouldReplaceTitle(meta.getTitle(), titleHint)) {
            meta.setTitle(resolveTitle(titleHint));
        }
        meta.setUpdatedAt(now);
        saveSessionMeta(userId, meta);
        return meta;
    }

    public void touchSessionByMemoryId(String memoryId, String titleHint) {
        ParsedMemoryKey parsed = parseMemoryKey(memoryId);
        if (parsed == null) {
            return;
        }
        touchSession(parsed.userId(), parsed.sessionId(), titleHint);
    }

    public boolean deleteSession(Long userId, String sessionId) {
        Long removedMeta = redisTemplate.opsForHash().delete(buildSessionMetaKey(userId), sessionId);
        Long removedOrder = redisTemplate.opsForZSet().remove(buildSessionOrderKey(userId), sessionId);
        redisTemplate.delete(buildMemoryKey(userId, sessionId));
        return (removedMeta != null && removedMeta > 0) || (removedOrder != null && removedOrder > 0);
    }

    public List<ChatSessionMessage> listMessages(Long userId, String sessionId) {
        List<ChatMessage> messages = chatMemoryStore.getMessages(buildMemoryKey(userId, sessionId));
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatSessionMessage> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            ChatSessionMessage mapped = mapMessage(message);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        refreshTtl(userId, sessionId);
        return result;
    }

    public String buildMemoryKey(Long userId, String sessionId) {
        return "chat:memory:user:" + userId + ":session:" + sessionId;
    }

    private void saveSessionMeta(Long userId, ChatSessionMeta meta) {
        redisTemplate.opsForHash().put(buildSessionMetaKey(userId), meta.getSessionId(), toJson(meta));
        redisTemplate.opsForZSet().add(buildSessionOrderKey(userId), meta.getSessionId(), meta.getUpdatedAt());
        refreshTtl(userId, meta.getSessionId());
    }

    private ChatSessionMeta getSessionMeta(Long userId, String sessionId) {
        Object raw = redisTemplate.opsForHash().get(buildSessionMetaKey(userId), sessionId);
        if (!(raw instanceof String json) || !StringUtils.hasText(json)) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            ChatSessionMeta meta = new ChatSessionMeta();
            meta.setSessionId(Objects.toString(map.get("sessionId"), sessionId));
            meta.setTitle(Objects.toString(map.get("title"), DEFAULT_TITLE));
            meta.setCreatedAt(toLong(map.get("createdAt")));
            meta.setUpdatedAt(toLong(map.get("updatedAt")));
            if (meta.getCreatedAt() <= 0) {
                meta.setCreatedAt(System.currentTimeMillis());
            }
            if (meta.getUpdatedAt() <= 0) {
                meta.setUpdatedAt(meta.getCreatedAt());
            }
            return meta;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void refreshTtl(Long userId, String sessionId) {
        Duration ttl = Duration.ofDays(Math.max(1, chatMemoryTtlDays));
        redisTemplate.expire(buildSessionMetaKey(userId), ttl);
        redisTemplate.expire(buildSessionOrderKey(userId), ttl);
        if (StringUtils.hasText(sessionId)) {
            redisTemplate.expire(buildMemoryKey(userId, sessionId), ttl);
        }
    }

    private String buildSessionMetaKey(Long userId) {
        return "chat:sessions:user:" + userId + ":meta";
    }

    private String buildSessionOrderKey(Long userId) {
        return "chat:sessions:user:" + userId + ":order";
    }

    private String resolveTitle(String titleHint) {
        if (!StringUtils.hasText(titleHint)) {
            return DEFAULT_TITLE;
        }
        String trimmed = titleHint.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 12 ? trimmed.substring(0, 12) : trimmed;
    }

    private boolean shouldReplaceTitle(String currentTitle, String titleHint) {
        if (!StringUtils.hasText(titleHint)) {
            return false;
        }
        return !StringUtils.hasText(currentTitle)
                || isDefaultTitle(currentTitle);
    }

    private boolean isEmptyDefaultSession(Long userId, ChatSessionMeta meta) {
        if (meta == null || !isDefaultTitle(meta.getTitle())) {
            return false;
        }
        List<ChatMessage> messages = chatMemoryStore.getMessages(buildMemoryKey(userId, meta.getSessionId()));
        if (messages == null || messages.isEmpty()) {
            return true;
        }
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isDefaultTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return true;
        }
        String trimmed = title.trim();
        return DEFAULT_TITLE.equals(trimmed)
                || "默认会话".equals(trimmed)
                || trimmed.startsWith("新会话");
    }

    private ParsedMemoryKey parseMemoryKey(String memoryId) {
        if (!StringUtils.hasText(memoryId)) {
            return null;
        }
        Matcher matcher = MEMORY_KEY_PATTERN.matcher(memoryId.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new ParsedMemoryKey(Long.parseLong(matcher.group(1)), matcher.group(2));
    }

    private ChatSessionMessage mapMessage(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return new ChatSessionMessage("user", sanitizeUserContent(userMessage.singleText()));
        }
        if (message instanceof AiMessage aiMessage) {
            return new ChatSessionMessage("assistant", sanitizeAssistantContent(aiMessage.text()));
        }
        if (message instanceof SystemMessage) {
            return null;
        }
        return null;
    }

    private String sanitizeAssistantContent(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String[] lines = text.replace("\r\n", "\n").split("\n");
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

    private String sanitizeUserContent(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.replace("\r\n", "\n");
        int userQuestionIndex = normalized.indexOf("\nUser question:\n");
        if (userQuestionIndex >= 0) {
            normalized = normalized.substring(userQuestionIndex + "\nUser question:\n".length());
        }
        String[] lines = normalized.split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) != '\n') {
                    cleaned.append('\n');
                }
                continue;
            }
            if (isInternalPromptLine(trimmed) || trimmed.startsWith("[Source:")) {
                break;
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString().trim();
    }

    private boolean isInternalPromptLine(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        for (String prefix : INTERNAL_PROMPT_PREFIXES) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String toJson(ChatSessionMeta meta) {
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize chat session meta", ex);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return 0L;
            }
        }
        return 0L;
    }

    private record ParsedMemoryKey(Long userId, String sessionId) {
    }

    public static class ChatSessionMeta {
        private String sessionId;
        private String title;
        private long createdAt;
        private long updatedAt;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ChatSessionMessage {
        private String role;
        private String content;

        public ChatSessionMessage() {
        }

        public ChatSessionMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
