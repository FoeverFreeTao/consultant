package com.zyt.consultant.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HybridContentRetriever implements ContentRetriever {

    private static final double MIN_KEYWORD_SCORE = 0.08;
    private static final String DEFAULT_SOURCE = "knowledge-base";
    private static final Pattern TEXT_FOR_EMBEDDING_PATTERN = Pattern.compile("\"text_for_embedding\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> SOURCE_KEYS = List.of(
            "source",
            "file_name",
            "fileName",
            Document.FILE_NAME,
            "absolute_file_path",
            "path",
            Document.ABSOLUTE_DIRECTORY_PATH,
            Document.URL,
            "url"
    );

    private final ContentRetriever vectorRetriever;
    private final List<TextSegment> keywordSegments;
    private final int keywordMaxResults;
    private final int finalMaxResults;
    private final double vectorWeight;
    private final double keywordWeight;

    public HybridContentRetriever(ContentRetriever vectorRetriever,
                                  List<TextSegment> keywordSegments,
                                  int keywordMaxResults,
                                  int finalMaxResults,
                                  double vectorWeight,
                                  double keywordWeight) {
        this.vectorRetriever = Objects.requireNonNull(vectorRetriever, "vectorRetriever cannot be null");
        this.keywordSegments = keywordSegments == null ? Collections.emptyList() : keywordSegments;
        this.keywordMaxResults = keywordMaxResults;
        this.finalMaxResults = finalMaxResults;
        this.vectorWeight = vectorWeight;
        this.keywordWeight = keywordWeight;
    }

    @Override
    public List<Content> retrieve(Query query) {
        ReferenceSourceContext.clear();
        List<Content> vectorResults = safeRetrieveVector(query);
        List<KeywordHit> keywordHits = keywordRetrieve(query.text());

        if (vectorResults.isEmpty() && keywordHits.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MergedHit> merged = new LinkedHashMap<>();

        for (Content content : vectorResults) {
            TextSegment segment = content.textSegment();
            String text = extractReadableText(segment.text());
            if (text.isBlank()) {
                continue;
            }
            String key = normalizeKey(text);
            MergedHit hit = merged.computeIfAbsent(
                    key,
                    ignored -> new MergedHit(text, extractSource(segment.metadata()), segment.metadata().copy())
            );
            hit.vectorScore = Math.max(hit.vectorScore, extractContentScore(content));
            hit.segmentMetadata = mergeMetadata(hit.segmentMetadata, segment.metadata());
        }

        for (KeywordHit keywordHit : keywordHits) {
            String cleanText = extractReadableText(keywordHit.text);
            if (cleanText.isBlank()) {
                continue;
            }
            String key = normalizeKey(cleanText);
            MergedHit hit = merged.computeIfAbsent(
                    key,
                    ignored -> new MergedHit(cleanText, keywordHit.source, keywordHit.metadata.copy())
            );
            hit.keywordScore = Math.max(hit.keywordScore, keywordHit.score);
            hit.segmentMetadata = mergeMetadata(hit.segmentMetadata, keywordHit.metadata);
            if (hit.source == null || hit.source.isBlank()) {
                hit.source = keywordHit.source;
            }
        }

        double maxVector = merged.values().stream().mapToDouble(h -> h.vectorScore).max().orElse(0D);
        double maxKeyword = merged.values().stream().mapToDouble(h -> h.keywordScore).max().orElse(0D);

        List<MergedHit> reranked = new ArrayList<>(merged.values());
        for (MergedHit hit : reranked) {
            double normalizedVector = maxVector > 0 ? hit.vectorScore / maxVector : 0D;
            double normalizedKeyword = maxKeyword > 0 ? hit.keywordScore / maxKeyword : 0D;
            double hybridBonus = (normalizedVector > 0 && normalizedKeyword > 0) ? 0.10 : 0D;
            hit.finalScore = (vectorWeight * normalizedVector) + (keywordWeight * normalizedKeyword) + hybridBonus;
        }

        reranked.sort(Comparator.comparingDouble((MergedHit h) -> h.finalScore).reversed());

        int limit = Math.max(1, finalMaxResults);
        List<Content> results = new ArrayList<>();
        for (MergedHit hit : reranked.subList(0, Math.min(limit, reranked.size()))) {
            String cleanedSource = nonBlankSource(hit.source);
            ReferenceSourceContext.addSource(cleanedSource);
            Metadata metadata = mergeMetadata(hit.segmentMetadata, Metadata.from("source", cleanedSource));
            TextSegment segment = TextSegment.from(toCitableText(hit.text, cleanedSource), metadata);

            Map<ContentMetadata, Object> contentMetadata = new LinkedHashMap<>();
            contentMetadata.put(ContentMetadata.SCORE, hit.finalScore);
            contentMetadata.put(ContentMetadata.RERANKED_SCORE, hit.finalScore);

            results.add(Content.from(segment, contentMetadata));
        }

        return results;
    }

    private List<Content> safeRetrieveVector(Query query) {
        try {
            List<Content> contents = vectorRetriever.retrieve(query);
            return contents == null ? Collections.emptyList() : contents;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<KeywordHit> keywordRetrieve(String queryText) {
        String normalizedQuery = normalizeForKeyword(queryText);
        if (normalizedQuery.isBlank() || keywordSegments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> queryBigrams = toBigrams(normalizedQuery);
        List<KeywordHit> hits = new ArrayList<>();

        for (TextSegment segment : keywordSegments) {
            String text = segment.text();
            String normalizedText = normalizeForKeyword(text);
            if (normalizedText.isBlank()) {
                continue;
            }

            double score = keywordScore(normalizedQuery, normalizedText, queryBigrams);
            if (score < MIN_KEYWORD_SCORE) {
                continue;
            }

            hits.add(new KeywordHit(text, extractSource(segment.metadata()), segment.metadata(), score));
        }

        hits.sort(Comparator.comparingDouble((KeywordHit h) -> h.score).reversed());
        int limit = Math.max(1, keywordMaxResults);
        return hits.subList(0, Math.min(limit, hits.size()));
    }

    private double keywordScore(String normalizedQuery, String normalizedText, Set<String> queryBigrams) {
        double containsBoost = normalizedText.contains(normalizedQuery) ? 1.0 : 0.0;

        if (queryBigrams.isEmpty()) {
            return containsBoost;
        }

        Set<String> textBigrams = toBigrams(normalizedText);
        if (textBigrams.isEmpty()) {
            return containsBoost;
        }

        int overlap = 0;
        for (String bigram : queryBigrams) {
            if (textBigrams.contains(bigram)) {
                overlap++;
            }
        }

        double recall = overlap / (double) queryBigrams.size();
        return Math.min(1.0, (0.85 * recall) + (0.15 * containsBoost));
    }

    private double extractContentScore(Content content) {
        Object scoreObj = content.metadata().get(ContentMetadata.SCORE);
        if (scoreObj instanceof Number number) {
            return number.doubleValue();
        }
        return 0D;
    }

    private Metadata mergeMetadata(Metadata first, Metadata second) {
        Metadata merged = first == null ? new Metadata() : first.copy();
        if (second != null) {
            merged.putAll(second.toMap());
        }
        return merged;
    }

    private String toCitableText(String text, String source) {
        return "Internal reference material. Summarize only, never expose raw text.\n"
                + "[Source:" + nonBlankSource(source) + "]\n"
                + text;
    }

    private String nonBlankSource(String source) {
        String cleaned = cleanSource(source);
        return (cleaned == null || cleaned.isBlank()) ? DEFAULT_SOURCE : cleaned;
    }

    private String extractSource(Metadata metadata) {
        if (metadata == null) {
            return "";
        }
        for (String key : SOURCE_KEYS) {
            String value = metadata.getString(key);
            if (value != null && !value.isBlank()) {
                return cleanSource(value);
            }
        }
        return "";
    }

    private String cleanSource(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }

        String s = source.trim();

        if (s.contains("%")) {
            try {
                s = URLDecoder.decode(s, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // keep original if decode fails
            }
        }

        s = s.replace("\\", "/");
        int queryIndex = s.indexOf('?');
        if (queryIndex >= 0) {
            s = s.substring(0, queryIndex);
        }
        int hashIndex = s.indexOf('#');
        if (hashIndex >= 0) {
            s = s.substring(0, hashIndex);
        }

        int lastSlash = s.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < s.length() - 1) {
            s = s.substring(lastSlash + 1);
        }

        s = s.replace("[", "")
                .replace("]", "")
                .trim();

        s = s.replaceAll("\\.(json|md|txt|pdf)$", "");

        return s;
    }

    private String normalizeForKeyword(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}\\p{L}\\p{Nd}]", "")
                .trim();
    }

    private Set<String> toBigrams(String text) {
        if (text == null || text.length() < 2) {
            return Collections.emptySet();
        }
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i < text.length() - 1; i++) {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }

    private String normalizeKey(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private String extractReadableText(String rawText) {
        if (rawText == null) {
            return "";
        }

        String normalized = rawText.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return "";
        }

        Matcher matcher = TEXT_FOR_EMBEDDING_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .trim();
        }

        List<String> lines = new ArrayList<>();
        for (String line : normalized.split("\n")) {
            String cleaned = line.trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (cleaned.startsWith("Answer using the following information")) {
                continue;
            }
            if (cleaned.startsWith("[Source:")) {
                continue;
            }
            if (isStructuralLine(cleaned)) {
                continue;
            }
            lines.add(cleaned.replaceAll("^\"|\"$", ""));
        }

        if (!lines.isEmpty()) {
            return String.join("\n", lines);
        }

        return normalized;
    }

    private boolean isStructuralLine(String line) {
        return line.equals("[")
                || line.equals("]")
                || line.equals("{")
                || line.equals("}")
                || line.equals("},")
                || line.equals("],")
                || line.matches("^\"[^\"]+\"\\s*:\\s*\\{?$")
                || line.matches("^\"[^\"]+\"\\s*:\\s*.+,$")
                || line.matches("^[\\[\\]{}\",]+$");
    }

    private static final class KeywordHit {
        private final String text;
        private final String source;
        private final Metadata metadata;
        private final double score;

        private KeywordHit(String text, String source, Metadata metadata, double score) {
            this.text = text;
            this.source = source;
            this.metadata = metadata == null ? new Metadata() : metadata.copy();
            this.score = score;
        }
    }

    private static final class MergedHit {
        private final String text;
        private String source;
        private Metadata segmentMetadata;
        private double vectorScore;
        private double keywordScore;
        private double finalScore;

        private MergedHit(String text, String source, Metadata segmentMetadata) {
            this.text = text;
            this.source = source;
            this.segmentMetadata = segmentMetadata == null ? new Metadata() : segmentMetadata.copy();
        }
    }
}
