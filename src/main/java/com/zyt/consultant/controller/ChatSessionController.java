package com.zyt.consultant.controller;

import com.zyt.consultant.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat/sessions")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid userId", null));
        }
        List<ChatSessionService.ChatSessionMeta> sessions = chatSessionService.listSessions(userId);
        return ResponseEntity.ok(buildResponse(true, "ok", sessions));
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> messages(@RequestParam("userId") Long userId,
                                                        @RequestParam("sessionId") String sessionId) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(sessionId)) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid params", null));
        }
        List<ChatSessionService.ChatSessionMessage> messages = chatSessionService.listMessages(userId, sessionId.trim());
        return ResponseEntity.ok(buildResponse(true, "ok", messages));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody SessionRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid userId", null));
        }
        ChatSessionService.ChatSessionMeta session = chatSessionService.createSession(request.getUserId(), request.getTitle());
        return ResponseEntity.ok(buildResponse(true, "created", session));
    }

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody SessionRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0 || !StringUtils.hasText(request.getSessionId())) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid params", null));
        }
        boolean deleted = chatSessionService.deleteSession(request.getUserId(), request.getSessionId().trim());
        return ResponseEntity.ok(buildResponse(deleted, deleted ? "deleted" : "not_found", null));
    }

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    public static class SessionRequest {
        private Long userId;
        private String sessionId;
        private String title;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

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
    }
}
