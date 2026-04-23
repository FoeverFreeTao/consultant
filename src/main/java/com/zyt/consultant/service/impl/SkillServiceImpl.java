package com.zyt.consultant.service.impl;

import com.zyt.consultant.entity.SkillOption;
import com.zyt.consultant.mapper.UserMapper;
import com.zyt.consultant.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String SKILL_RESOURCE_PATTERN = "classpath*:skills/*.md";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:^user-(\\d+)$)|(?:^chat:memory:user:(\\d+):session:[\\w-]+$)");

    private final Map<String, SkillDefinition> skillCatalog = new LinkedHashMap<>();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @PostConstruct
    public void initSkillCatalog() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SKILL_RESOURCE_PATTERN);
            Map<String, SkillDefinition> loaded = new LinkedHashMap<>();
            for (Resource resource : resources) {
                SkillDefinition definition = parseSkillResource(resource);
                if (definition != null) {
                    loaded.put(definition.option().getId(), definition);
                }
            }
            if (loaded.isEmpty()) {
                throw new IllegalStateException("No skill markdown files found under classpath:skills/");
            }
            skillCatalog.clear();
            skillCatalog.putAll(loaded);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load skill markdown files", ex);
        }
    }

    @Override
    public List<SkillOption> listAllSkills() {
        return skillCatalog.values().stream().map(SkillDefinition::option).collect(Collectors.toCollection(ArrayList::new));
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
            SkillDefinition definition = skillCatalog.get(option.getId());
            if (definition == null) {
                continue;
            }
            sb.append("[")
                    .append(option.getId())
                    .append("] ")
                    .append(option.getName())
                    .append("\n")
                    .append(definition.markdownContent().trim())
                    .append("\n\n");
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
            SkillDefinition definition = skillCatalog.get(id);
            if (definition != null) {
                result.add(definition.option());
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
            if (skillCatalog.containsKey(cleaned)) {
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
        Matcher matcher = USER_ID_PATTERN.matcher(memoryId.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            String matched = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return Long.parseLong(matched);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private SkillDefinition parseSkillResource(Resource resource) throws IOException {
        String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        ParsedMarkdown parsed = splitFrontMatter(text);
        Map<String, String> metadata = parseMetadata(parsed.frontMatter());
        String id = metadata.get("id");
        String name = metadata.get("name");
        String description = metadata.get("description");
        if (!StringUtils.hasText(id) || !StringUtils.hasText(name) || !StringUtils.hasText(description)) {
            throw new IllegalStateException("Skill markdown missing required metadata: " + resource.getFilename());
        }
        SkillOption option = new SkillOption(id.trim(), name.trim(), description.trim(), parsed.body().trim());
        return new SkillDefinition(option, parsed.body().trim());
    }

    private ParsedMarkdown splitFrontMatter(String text) {
        String normalized = text.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            throw new IllegalStateException("Skill markdown must start with front matter");
        }
        int secondDelimiter = normalized.indexOf("\n---\n", 4);
        if (secondDelimiter < 0) {
            throw new IllegalStateException("Skill markdown front matter is not closed");
        }
        String frontMatter = normalized.substring(4, secondDelimiter);
        String body = normalized.substring(secondDelimiter + 5).trim();
        return new ParsedMarkdown(frontMatter, body);
    }

    private Map<String, String> parseMetadata(String frontMatter) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : frontMatter.split("\n")) {
            if (!StringUtils.hasText(line) || !line.contains(":")) {
                continue;
            }
            int index = line.indexOf(':');
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            metadata.put(key, value);
        }
        return metadata;
    }

    private record ParsedMarkdown(String frontMatter, String body) {
    }

    private record SkillDefinition(SkillOption option, String markdownContent) {
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
