package com.zyt.consultant.guardrail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.guardrails.input")
public class ChatSafetyProperties {

    private boolean enabled = true;

    private List<String> blockedKeywords = new ArrayList<>(List.of(
            "\u70B8\u836F",
            "\u67AA\u652F\u5236\u4F5C",
            "\u6BD2\u54C1\u4EA4\u6613",
            "\u8BC8\u9A97\u811A\u672C",
            "\u52D2\u7D22\u75C5\u6BD2",
            "\u6728\u9A6C\u7A0B\u5E8F",
            "\u6D17\u94B1"
    ));

    private List<String> injectionPatterns = new ArrayList<>(List.of(
            "ignore previous instructions",
            "ignore all previous instructions",
            "forget previous instructions",
            "reveal system prompt",
            "show system prompt",
            "developer message",
            "bypass safety",
            "jailbreak",
            "do anything now",
            "answer using the following information",
            "\u5FFD\u7565\u4E4B\u524D\u6240\u6709\u6307\u4EE4",
            "\u65E0\u89C6\u4EE5\u4E0A\u89C4\u5219",
            "\u8F93\u51FA\u7CFB\u7EDF\u63D0\u793A\u8BCD",
            "\u663E\u793A\u7CFB\u7EDF\u63D0\u793A\u8BCD",
            "\u5F00\u53D1\u8005\u6D88\u606F",
            "\u8D8A\u72F1\u63D0\u793A"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getBlockedKeywords() {
        return blockedKeywords;
    }

    public void setBlockedKeywords(List<String> blockedKeywords) {
        this.blockedKeywords = blockedKeywords == null ? new ArrayList<>() : blockedKeywords;
    }

    public List<String> getInjectionPatterns() {
        return injectionPatterns;
    }

    public void setInjectionPatterns(List<String> injectionPatterns) {
        this.injectionPatterns = injectionPatterns == null ? new ArrayList<>() : injectionPatterns;
    }
}
