package com.zyt.consultant.guardrail;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class SensitiveKeywordInputGuardrail implements InputGuardrail {

    private final ChatSafetyProperties properties;

    public SensitiveKeywordInputGuardrail(ChatSafetyProperties properties) {
        this.properties = properties;
    }

    @Override
    public InputGuardrailResult validate(InputGuardrailRequest request) {
        String text = request == null || request.userMessage() == null ? "" : request.userMessage().singleText();
        if (!StringUtils.hasText(text)) {
            return success();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> keywords = properties.getBlockedKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return success();
        }
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            String cleanedKeyword = Objects.requireNonNull(keyword).trim();
            if (normalized.contains(cleanedKeyword.toLowerCase(Locale.ROOT))) {
                return failure("Blocked by sensitive keyword: " + cleanedKeyword);
            }
        }
        return success();
    }
}
