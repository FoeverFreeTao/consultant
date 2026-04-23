package com.zyt.consultant.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailExecutor;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.config.InputGuardrailsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatInputGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(ChatInputGuardrailService.class);

    private final ChatSafetyProperties properties;
    private final InputGuardrailExecutor executor;

    public ChatInputGuardrailService(ChatSafetyProperties properties, List<InputGuardrail> inputGuardrails) {
        this.properties = properties;
        this.executor = InputGuardrailExecutor.builder()
                .config(InputGuardrailsConfig.builder().build())
                .guardrails(inputGuardrails)
                .build();
    }

    public String validate(String message) {
        if (!properties.isEnabled() || !StringUtils.hasText(message)) {
            return message;
        }
        try {
            InputGuardrailRequest request = InputGuardrailRequest.builder()
                    .userMessage(UserMessage.from(message))
                    .commonParams(GuardrailRequestParams.builder()
                            .userMessageTemplate(message)
                            .variables(Map.of())
                            .build())
                    .build();
            InputGuardrailResult result = executor.execute(request);
            if (!result.isSuccess()) {
                throw new InputRiskBlockedException(buildFailureMessage(result));
            }
            if (StringUtils.hasText(result.successfulText())) {
                return result.successfulText();
            }
            return message;
        } catch (InputRiskBlockedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Input guardrail execution failed, fallback to allow message. message={}", message, ex);
            return message;
        }
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
