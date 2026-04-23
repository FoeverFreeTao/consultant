package com.zyt.consultant.controller;

import com.zyt.consultant.entity.SkillOption;
import com.zyt.consultant.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/skills")
public class SkillController {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:^user-(\\d+)$)|(?:^chat:memory:user:(\\d+):session:[\\w-]+$)");

    @Autowired
    private SkillService skillService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        List<SkillOption> skills = skillService.listAllSkills();
        return ResponseEntity.ok(buildResponse(true, "ok", skills));
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userSelected(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid userId", null));
        }
        List<SkillOption> skills = skillService.getUserSkills(userId);
        return ResponseEntity.ok(buildResponse(true, "ok", skills));
    }

    @PostMapping("/user/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody ApplySkillRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid request", null));
        }
        boolean ok = skillService.applyUserSkills(request.getUserId(), request.getSkillIds());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(false, "user not found", null));
        }
        return ResponseEntity.ok(buildResponse(true, "applied", skillService.getUserSkills(request.getUserId())));
    }

    @PostMapping("/user/apply-by-memory")
    public ResponseEntity<Map<String, Object>> applyByMemory(@RequestBody ApplyByMemoryRequest request) {
        if (request == null || !StringUtils.hasText(request.getMemoryId())) {
            return ResponseEntity.badRequest().body(buildResponse(false, "invalid request", null));
        }
        Long userId = parseUserId(request.getMemoryId().trim());
        if (userId == null) {
            return ResponseEntity.badRequest().body(buildResponse(false, "memoryId must be like user-{id}", null));
        }
        boolean ok = skillService.applyUserSkills(userId, request.getSkillIds());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(false, "user not found", null));
        }
        return ResponseEntity.ok(buildResponse(true, "applied", skillService.getUserSkills(userId)));
    }

    private Long parseUserId(String memoryId) {
        Matcher matcher = USER_ID_PATTERN.matcher(memoryId);
        if (!matcher.matches()) {
            return null;
        }
        try {
            String matched = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return Long.parseLong(matched);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    public static class ApplySkillRequest {
        private Long userId;
        private List<String> skillIds;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<String> getSkillIds() {
            return skillIds;
        }

        public void setSkillIds(List<String> skillIds) {
            this.skillIds = skillIds;
        }
    }

    public static class ApplyByMemoryRequest {
        private String memoryId;
        private List<String> skillIds;

        public String getMemoryId() {
            return memoryId;
        }

        public void setMemoryId(String memoryId) {
            this.memoryId = memoryId;
        }

        public List<String> getSkillIds() {
            return skillIds;
        }

        public void setSkillIds(List<String> skillIds) {
            this.skillIds = skillIds;
        }
    }
}
