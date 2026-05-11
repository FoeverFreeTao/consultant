package com.zyt.consultant.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rag.search")
public class RagSearchProperties {

    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:9200";
    private String username = "";
    private String password = "";
    private String indexName = "consultant_knowledge";
    private boolean autoIndex = true;
    private int requestTimeoutMs = 1500;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public boolean isAutoIndex() {
        return autoIndex;
    }

    public void setAutoIndex(boolean autoIndex) {
        this.autoIndex = autoIndex;
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
