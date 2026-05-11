package com.zyt.consultant.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rag.rerank")
public class RagRerankProperties {

    private boolean enabled = true;
    private String baseUrl = "https://dashscope.aliyuncs.com";
    private String apiKey = "";
    private String modelName = "qwen3-rerank";
    private int candidateLimit = 32;
    private int topN = 4;
    private int maxDocumentChars = 3500;
    private int requestTimeoutMs = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public int getMaxDocumentChars() {
        return maxDocumentChars;
    }

    public void setMaxDocumentChars(int maxDocumentChars) {
        this.maxDocumentChars = maxDocumentChars;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public Duration requestTimeout() {
        return Duration.ofMillis(Math.max(100, requestTimeoutMs));
    }
}
