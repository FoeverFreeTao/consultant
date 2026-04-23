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
public class PromptInjectionInputGuardrail implements InputGuardrail {

    private final ChatSafetyProperties properties;

    public PromptInjectionInputGuardrail(ChatSafetyProperties properties) {
        this.properties = properties;
    }

    @Override
    public InputGuardrailResult validate(InputGuardrailRequest request) {
        String text = request == null || request.userMessage() == null ? "" : request.userMessage().singleText();
        if (!StringUtils.hasText(text)) {
            return success();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> patterns = properties.getInjectionPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return success();
        }
        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            String cleanedPattern = Objects.requireNonNull(pattern).trim();
            if (normalized.contains(cleanedPattern.toLowerCase(Locale.ROOT))) {
                return failure("Blocked by prompt injection pattern: " + cleanedPattern);
            }
        }
        return success();
    }
}
