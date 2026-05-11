package com.zyt.consultant.rag;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashScopeCrossEncoderReranker implements CrossEncoderReranker {

    private static final Logger log = LoggerFactory.getLogger(DashScopeCrossEncoderReranker.class);

    private final RagRerankProperties properties;
    private final WebClient webClient;

    public DashScopeCrossEncoderReranker(RagRerankProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public List<RerankScore> rerank(String queryText, List<String> documents, int topN) {
        if (!properties.isEnabled()
                || queryText == null
                || queryText.isBlank()
                || documents == null
                || documents.isEmpty()
                || effectiveApiKey().isBlank()) {
            return List.of();
        }

        List<String> trimmedDocuments = documents.stream()
                .map(this::trimDocument)
                .toList();
        int requestedTopN = Math.max(1, Math.min(topN, trimmedDocuments.size()));

        try {
            JsonNode response = isOpenAiCompatibleRerankModel()
                    ? callOpenAiCompatibleEndpoint(queryText, trimmedDocuments, requestedTopN)
                    : callDashScopeTextRerankEndpoint(queryText, trimmedDocuments, requestedTopN);
            return parseScores(response).stream()
                    .filter(score -> score.index() >= 0 && score.index() < documents.size())
                    .sorted(Comparator.comparingDouble(RerankScore::score).reversed())
                    .toList();
        } catch (Exception ex) {
            log.warn("Cross-encoder rerank failed. model={}", properties.getModelName(), ex);
            return List.of();
        }
    }

    private JsonNode callDashScopeTextRerankEndpoint(String queryText, List<String> documents, int topN) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModelName());
        body.put("input", Map.of(
                "query", queryText,
                "documents", documents
        ));
        body.put("parameters", Map.of(
                "top_n", topN,
                "return_documents", false
        ));

        return webClient.post()
                .uri("/api/v1/services/rerank/text-rerank/text-rerank")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.requestTimeout());
    }

    private JsonNode callOpenAiCompatibleEndpoint(String queryText, List<String> documents, int topN) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModelName());
        body.put("query", queryText);
        body.put("documents", documents);
        body.put("top_n", topN);

        return webClient.post()
                .uri("/compatible-api/v1/reranks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.requestTimeout());
    }

    private List<RerankScore> parseScores(JsonNode root) {
        if (root == null) {
            return List.of();
        }
        JsonNode results = root.path("output").path("results");
        if (!results.isArray()) {
            results = root.path("results");
        }
        if (!results.isArray()) {
            return List.of();
        }
        List<RerankScore> scores = new ArrayList<>();
        for (JsonNode result : results) {
            int index = result.path("index").asInt(-1);
            double score = result.has("relevance_score")
                    ? result.path("relevance_score").asDouble(0D)
                    : result.path("score").asDouble(0D);
            scores.add(new RerankScore(index, score));
        }
        return scores;
    }

    private boolean isOpenAiCompatibleRerankModel() {
        String modelName = properties.getModelName() == null ? "" : properties.getModelName();
        return modelName.startsWith("qwen3-rerank");
    }

    private String trimDocument(String document) {
        if (document == null) {
            return "";
        }
        int limit = Math.max(200, properties.getMaxDocumentChars());
        return document.length() <= limit ? document : document.substring(0, limit);
    }

    private String effectiveApiKey() {
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            return properties.getApiKey();
        }
        String dashScopeApiKey = System.getenv("DASHSCOPE_API_KEY");
        if (dashScopeApiKey != null && !dashScopeApiKey.isBlank()) {
            return dashScopeApiKey;
        }
        String apiKey = System.getenv("API_KEY");
        return apiKey == null ? "" : apiKey;
    }
}
