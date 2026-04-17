package com.zyt.consultant.service.impl;

import com.zyt.consultant.entity.SkillOption;
import com.zyt.consultant.mapper.UserMapper;
import com.zyt.consultant.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillServiceImpl implements SkillService {

    private static final String SKILL_KEY_PREFIX = "consultant:user:skills:";

    private static final Map<String, SkillOption> SKILL_CATALOG;

    static {
        Map<String, SkillOption> map = new LinkedHashMap<>();
        map.put("fat_loss", new SkillOption(
                "fat_loss",
                "Fat Loss Coach",
                "Prefer lower-calorie, higher-satiety suggestions.",
                "Prioritize fat-loss strategy: provide lower-calorie and high-satiety meal options, and clearly explain substitutions."
        ));
        map.put("muscle_gain", new SkillOption(
                "muscle_gain",
                "Muscle Gain Coach",
                "Prefer higher-protein distribution and recovery suggestions.",
                "Prioritize muscle-gain strategy: optimize protein distribution through the day and add post-workout recovery meal suggestions."
        ));
        map.put("low_sodium", new SkillOption(
                "low_sodium",
                "Low Sodium",
                "Reduce sodium and processed-food recommendations.",
                "Prioritize low-sodium strategy: avoid high-salt foods and provide practical seasoning alternatives."
        ));
        map.put("diabetes_friendly", new SkillOption(
                "diabetes_friendly",
                "Blood Sugar Friendly",
                "Prefer lower-GI carbs and stable blood sugar guidance.",
                "Prioritize blood-sugar-friendly strategy: recommend lower-GI carbohydrates and explain meal order/timing."
        ));
        map.put("quick_meal", new SkillOption(
                "quick_meal",
                "Quick Meal",
                "Provide practical, time-saving meals.",
                "Prioritize quick-meal strategy: keep recipes simple, around 10-20 minutes, and use common ingredients."
        ));
        SKILL_CATALOG = Collections.unmodifiableMap(map);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<SkillOption> listAllSkills() {
        return new ArrayList<>(SKILL_CATALOG.values());
    }

    @Override
    public List<SkillOption> getUserSkills(Long userId) {
        if (!isValidUser(userId)) {
            return Collections.emptyList();
        }
        Set<String> savedIds = stringRedisTemplate.opsForSet().members(buildRedisKey(userId));
        return toSkillList(savedIds);
    }

    @Override
    public boolean applyUserSkills(Long userId, List<String> skillIds) {
        if (!isValidUser(userId)) {
            return false;
        }
        String redisKey = buildRedisKey(userId);
        stringRedisTemplate.delete(redisKey);
        Set<String> sanitized = sanitizeSkillIds(skillIds);
        if (!sanitized.isEmpty()) {
            stringRedisTemplate.opsForSet().add(redisKey, sanitized.toArray(new String[0]));
        }
        return true;
    }

    @Override
    public String buildSkillPromptByMemoryId(String memoryId) {
        Long userId = parseUserIdFromMemory(memoryId);
        if (userId == null) {
            return "";
        }
        List<SkillOption> userSkills = getUserSkills(userId);
        return buildSkillPromptBySkillIds(userSkills.stream().map(SkillOption::getId).collect(Collectors.toList()));
    }

    @Override
    public String buildSkillPromptBySkillIds(List<String> skillIds) {
        Set<String> ids = sanitizeSkillIds(skillIds);
        if (ids.isEmpty()) {
            return "";
        }
        List<SkillOption> selected = toSkillList(ids);
        if (selected.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("User selected skills (must follow):\n");
        for (SkillOption option : selected) {
            sb.append("- ").append(option.getName()).append(": ").append(option.getPromptInstruction()).append("\n");
        }
        sb.append("When skills conflict, keep safety first and explain trade-offs briefly.");
        return sb.toString();
    }

    private List<SkillOption> toSkillList(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SkillOption> result = new ArrayList<>();
        for (String id : ids) {
            SkillOption option = SKILL_CATALOG.get(id);
            if (option != null) {
                result.add(option);
            }
        }
        return result;
    }

    private Set<String> sanitizeSkillIds(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String id : skillIds) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            String cleaned = id.trim();
            if (SKILL_CATALOG.containsKey(cleaned)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private boolean isValidUser(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        Long count = userMapper.countById(userId);
        return count != null && count > 0;
    }

    private String buildRedisKey(Long userId) {
        return SKILL_KEY_PREFIX + userId;
    }

    private Long parseUserIdFromMemory(String memoryId) {
        if (!StringUtils.hasText(memoryId)) {
            return null;
        }
        String text = memoryId.trim();
        if (!text.startsWith("user-")) {
            return null;
        }
        String idPart = text.substring("user-".length());
        if (!StringUtils.hasText(idPart)) {
            return null;
        }
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static List<String> parseSkillIds(String skillIds) {
        if (!StringUtils.hasText(skillIds)) {
            return Collections.emptyList();
        }
        return Arrays.stream(skillIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}

