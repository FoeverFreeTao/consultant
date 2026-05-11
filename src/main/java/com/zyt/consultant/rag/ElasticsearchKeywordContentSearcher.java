package com.zyt.consultant.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchKeywordContentSearcher implements KeywordContentSearcher, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKeywordContentSearcher.class);

    private final RagSearchProperties properties;
    private final List<TextSegment> segments;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ElasticsearchKeywordContentSearcher(RagSearchProperties properties,
                                               List<TextSegment> segments,
                                               ObjectMapper objectMapper) {
        this.properties = properties;
        this.segments = segments == null ? Collections.emptyList() : segments;
        this.objectMapper = objectMapper;
        this.webClient = buildWebClient(properties);
        validateIndexName(properties.getIndexName());
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled() || !properties.isAutoIndex() || segments.isEmpty()) {
            return;
        }
        if (!isElasticsearchAvailable()) {
            log.warn("Elasticsearch is unavailable. keyword recall is disabled for now. baseUrl={}",
                    properties.getBaseUrl());
            return;
        }
        try {
            ensureIndex();
            bulkIndex(segments);
            log.info("Elasticsearch keyword index refreshed. index={}, segments={}",
                    properties.getIndexName(), segments.size());
        } catch (Exception ex) {
            log.warn("Failed to refresh Elasticsearch keyword index. keyword recall will be skipped. index={}, reason={}",
                    properties.getIndexName(), rootMessage(ex));
        }
    }

    @Override
    public List<RetrievedTextSegment> search(String queryText, int maxResults) {
        if (!properties.isEnabled() || queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }
        int size = Math.max(1, maxResults);
        Map<String, Object> request = Map.of(
                "size", size,
                "_source", List.of("text", "source", "metadata"),
                "query", Map.of(
                        "bool", Map.of(
                                "should", List.of(
                                        Map.of("match_phrase", Map.of("text", Map.of("query", queryText, "boost", 2.0))),
                                        Map.of("multi_match", Map.of(
                                                "query", queryText,
                                                "fields", List.of("text^2", "source"),
                                                "type", "best_fields",
                                                "operator", "or"
                                        ))
                                ),
                                "minimum_should_match", 1
                        )
                )
        );

        try {
            JsonNode root = webClient.post()
                    .uri(uriBuilder -> uriBuilder.pathSegment(properties.getIndexName(), "_search").build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(properties.requestTimeout());

            if (root == null) {
                return Collections.emptyList();
            }
            return parseHits(root);
        } catch (Exception ex) {
            log.warn("Elasticsearch keyword recall failed. query={}, reason={}", queryText, rootMessage(ex));
            return Collections.emptyList();
        }
    }

    private boolean isElasticsearchAvailable() {
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri("/")
                    .exchangeToMono(response -> response.releaseBody()
                            .thenReturn(response.statusCode().is2xxSuccessful()))
                    .onErrorReturn(false)
                    .block(properties.requestTimeout()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void ensureIndex() {
        boolean exists = Boolean.TRUE.equals(webClient.head()
                .uri(uriBuilder -> uriBuilder.pathSegment(properties.getIndexName()).build())
                .exchangeToMono(response -> response.releaseBody()
                        .thenReturn(response.statusCode().is2xxSuccessful()))
                .onErrorReturn(false)
                .block(properties.requestTimeout()));
        if (exists) {
            return;
        }

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("settings", Map.of(
                "number_of_shards", 1,
                "number_of_replicas", 0
        ));
        mapping.put("mappings", Map.of(
                "properties", Map.of(
                        "text", Map.of("type", "text", "analyzer", "standard"),
                        "source", Map.of("type", "keyword"),
                        "metadata", Map.of("type", "object", "enabled", true)
                )
        ));

        webClient.put()
                .uri(uriBuilder -> uriBuilder.pathSegment(properties.getIndexName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapping)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.requestTimeout());
    }

    private void bulkIndex(List<TextSegment> sourceSegments) throws Exception {
        StringBuilder ndjson = new StringBuilder();
        for (TextSegment segment : sourceSegments) {
            if (segment == null || segment.text() == null || segment.text().isBlank()) {
                continue;
            }
            Map<String, Object> action = Map.of("index", Map.of("_id", stableId(segment)));
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("text", segment.text());
            document.put("source", sourceOf(segment));
            document.put("metadata", segment.metadata() == null ? Map.of() : segment.metadata().toMap());
            ndjson.append(objectMapper.writeValueAsString(action)).append('\n');
            ndjson.append(objectMapper.writeValueAsString(document)).append('\n');
        }
        if (ndjson.isEmpty()) {
            return;
        }

        JsonNode response = webClient.post()
                .uri(uriBuilder -> uriBuilder.pathSegment(properties.getIndexName(), "_bulk")
                        .queryParam("refresh", "true")
                        .build())
                .contentType(MediaType.APPLICATION_NDJSON)
                .bodyValue(ndjson.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.requestTimeout().multipliedBy(4));

        if (response != null && response.path("errors").asBoolean(false)) {
            log.warn("Elasticsearch bulk index completed with item errors. index={}", properties.getIndexName());
        }
    }

    private List<RetrievedTextSegment> parseHits(JsonNode root) {
        JsonNode hits = root.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<RetrievedTextSegment> results = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            String text = source.path("text").asText("");
            if (text.isBlank()) {
                continue;
            }
            Metadata metadata = new Metadata();
            JsonNode metadataNode = source.path("metadata");
            if (metadataNode.isObject()) {
                metadataNode.fields().forEachRemaining(entry -> putMetadataValue(metadata, entry.getKey(), entry.getValue()));
            }
            String sourceName = source.path("source").asText("");
            results.add(new RetrievedTextSegment(text, sourceName, metadata, hit.path("_score").asDouble(0D)));
        }
        return results;
    }

    private void putMetadataValue(Metadata metadata, String key, JsonNode value) {
        if (value == null || value.isNull()) {
            metadata.put(key, "");
            return;
        }
        if (value.isInt()) {
            metadata.put(key, value.asInt());
            return;
        }
        if (value.isLong()) {
            metadata.put(key, value.asLong());
            return;
        }
        if (value.isFloat() || value.isDouble() || value.isBigDecimal()) {
            metadata.put(key, value.asDouble());
            return;
        }
        if (value.isBoolean()) {
            metadata.put(key, Boolean.toString(value.asBoolean()));
            return;
        }
        metadata.put(key, value.asText());
    }

    private String sourceOf(TextSegment segment) {
        if (segment.metadata() == null) {
            return "";
        }
        String source = segment.metadata().getString("source");
        if (source != null && !source.isBlank()) {
            return source;
        }
        String fileName = segment.metadata().getString("file_name");
        return fileName == null ? "" : fileName;
    }

    private String stableId(TextSegment segment) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String raw = sourceOf(segment) + "\n" + segment.text();
        return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private WebClient buildWebClient(RagSearchProperties searchProperties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(searchProperties.getBaseUrl());
        if (searchProperties.getUsername() != null && !searchProperties.getUsername().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION,
                    "Basic " + HttpHeaders.encodeBasicAuth(
                            searchProperties.getUsername(),
                            searchProperties.getPassword() == null ? "" : searchProperties.getPassword(),
                            StandardCharsets.UTF_8
                    ));
        }
        return builder.build();
    }

    private void validateIndexName(String indexName) {
        if (indexName == null || !indexName.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid Elasticsearch index name: " + indexName);
        }
    }

    private String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }
}
