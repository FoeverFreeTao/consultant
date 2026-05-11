package com.zyt.consultant.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatInputGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(ChatInputGuardrailService.class);

    private final ChatSafetyProperties properties;
    private final List<InputGuardrail> inputGuardrails;

    public ChatInputGuardrailService(ChatSafetyProperties properties, List<InputGuardrail> inputGuardrails) {
        this.properties = properties;
        this.inputGuardrails = inputGuardrails == null ? List.of() : new ArrayList<>(inputGuardrails);
    }

    public String validate(String message) {
        if (!properties.isEnabled() || !StringUtils.hasText(message)) {
            return message;
        }
        try {
            String currentMessage = message;
            for (InputGuardrail guardrail : inputGuardrails) {
                InputGuardrailResult result = guardrail.validate(buildRequest(currentMessage));
                if (result == null) {
                    continue;
                }
                if (!result.isSuccess()) {
                    throw new InputRiskBlockedException(buildFailureMessage(result));
                }
                if (StringUtils.hasText(result.successfulText())) {
                    currentMessage = result.successfulText();
                }
            }
            return currentMessage;
        } catch (InputRiskBlockedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Input guardrail execution failed, fallback to allow message. message={}, reason={}", message, ex.getMessage());
            return message;
        }
    }

    private InputGuardrailRequest buildRequest(String message) {
        return InputGuardrailRequest.builder()
                .userMessage(UserMessage.from(message))
                .commonParams(GuardrailRequestParams.builder()
                        .userMessageTemplate(message)
                        .variables(Map.of())
                        .build())
                .build();
    }

    private String buildFailureMessage(InputGuardrailResult result) {
        if (result == null || result.failures() == null || result.failures().isEmpty()) {
            return "Blocked by input guardrail";
        }
        return result.failures().stream()
                .map(failure -> failure == null ? "" : failure.message())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("; "));
    }
}
