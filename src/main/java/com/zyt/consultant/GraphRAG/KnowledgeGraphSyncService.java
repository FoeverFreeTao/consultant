package com.zyt.consultant.GraphRAG;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(prefix = "app.knowledge-graph.neo4j", name = "enabled", havingValue = "true")
public class KnowledgeGraphSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphSyncService.class);
    private static final Pattern TEXT_FOR_EMBEDDING_PATTERN = Pattern.compile("\"text_for_embedding\"\\s*:\\s*\"([^\"]+)\"");

    private final Driver driver;
    private final KnowledgeGraphProperties graphProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.vector-store.pgvector.host:127.0.0.1}")
    private String pgHost;

    @Value("${app.vector-store.pgvector.port:5432}")
    private Integer pgPort;

    @Value("${app.vector-store.pgvector.user:postgres}")
    private String pgUser;

    @Value("${app.vector-store.pgvector.password:123456}")
    private String pgPassword;

    @Value("${app.vector-store.pgvector.database:vector_db}")
    private String pgDatabase;

    @Value("${app.vector-store.pgvector.table:consultant_embedding}")
    private String pgTable;

    public KnowledgeGraphSyncService(Driver driver,
                                     KnowledgeGraphProperties graphProperties,
                                     ObjectMapper objectMapper) {
        this.driver = driver;
        this.graphProperties = graphProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (graphProperties.isAutoSync()) {
            try {
                SyncResult result = sync();
                log.info("Neo4j knowledge graph auto sync finished. {}", result);
            } catch (Exception ex) {
                log.warn("Neo4j knowledge graph auto sync failed, application will continue startup. " +
                        "Please check Neo4j connection and call /knowledge-graph/sync manually.", ex);
            }
        }
    }

    public SyncResult sync() {
        ensureSchema();

        List<FoodDocument> foods = loadFoodDocuments();
        syncFoodDocuments(foods);

        List<EmbeddingChunk> chunks;
        try {
            chunks = loadEmbeddingChunks();
        } catch (Exception ex) {
            log.warn("Food graph synced, but pgvector chunks sync was skipped.", ex);
            return new SyncResult(
                    foods.size(),
                    0,
                    false,
                    "Food graph synced. Pgvector chunks were skipped: " + ex.getMessage()
            );
        }

        try (org.neo4j.driver.Session session = driver.session(sessionConfig())) {
            writeChunks(session, chunks, foods);
            if (graphProperties.isCreateSimilarityRelations()) {
                writeSimilarityRelations(session, chunks);
            }
        }

        return new SyncResult(foods.size(), chunks.size(), graphProperties.isCreateSimilarityRelations(), "");
    }

    public SyncResult syncFoods() {
        ensureSchema();
        List<FoodDocument> foods = loadFoodDocuments();
        syncFoodDocuments(foods);
        return new SyncResult(foods.size(), 0, false, "");
    }

    private void syncFoodDocuments(List<FoodDocument> foods) {
        try (org.neo4j.driver.Session session = driver.session(sessionConfig())) {
            writeFoods(session, foods);
        }
    }

    private void ensureSchema() {
        try (org.neo4j.driver.Session session = driver.session(sessionConfig())) {
            int dimension = Math.max(1, graphProperties.getVectorDimension());
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT kg_food_id IF NOT EXISTS FOR (n:Food) REQUIRE n.id IS UNIQUE");
                tx.run("CREATE CONSTRAINT kg_category_name IF NOT EXISTS FOR (n:Category) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT kg_feature_name IF NOT EXISTS FOR (n:Feature) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT kg_nutrient_name IF NOT EXISTS FOR (n:Nutrient) REQUIRE n.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT kg_chunk_id IF NOT EXISTS FOR (n:KnowledgeChunk) REQUIRE n.id IS UNIQUE");
                tx.run(String.format("""
                        CREATE VECTOR INDEX consultant_chunk_embedding IF NOT EXISTS
                        FOR (n:KnowledgeChunk) ON (n.embedding)
                        OPTIONS {indexConfig: {
                          `vector.dimensions`: %d,
                          `vector.similarity_function`: 'cosine'
                        }}
                        """, dimension));
                return null;
            });
        }
    }

    private void writeFoods(org.neo4j.driver.Session session, List<FoodDocument> foods) {
        for (List<FoodDocument> batch : batches(foods, graphProperties.getBatchSize())) {
            List<Map<String, Object>> rows = batch.stream()
                    .map(FoodDocument::toMap)
                    .toList();

            session.executeWrite(tx -> {
                tx.run("""
                        UNWIND $rows AS row
                        MERGE (food:Food {id: row.id})
                        SET food.name = row.name,
                            food.category = row.category,
                            food.text = row.textForEmbedding,
                            food.nutrition = row.nutrition
                        MERGE (category:Category {name: row.category})
                        MERGE (food)-[:BELONGS_TO]->(category)
                        WITH food, row
                        UNWIND row.features AS featureName
                        MERGE (feature:Feature {name: featureName})
                        MERGE (food)-[:HAS_FEATURE]->(feature)
                        """, Map.of("rows", rows));

                tx.run("""
                        UNWIND $rows AS row
                        MATCH (food:Food {id: row.id})
                        UNWIND row.nutrientRows AS nutrientRow
                        MERGE (nutrient:Nutrient {name: nutrientRow.name})
                        MERGE (food)-[rel:HAS_NUTRIENT]->(nutrient)
                        SET rel.amount = nutrientRow.amount,
                            rel.unit = nutrientRow.unit,
                            rel.per = '100g'
                        """, Map.of("rows", rows));
                return null;
            });
        }
    }

    private void writeChunks(org.neo4j.driver.Session session,
                             List<EmbeddingChunk> chunks,
                             List<FoodDocument> foods) {
        Map<String, FoodDocument> foodByName = new LinkedHashMap<>();
        for (FoodDocument food : foods) {
            foodByName.put(food.name(), food);
        }

        for (List<EmbeddingChunk> batch : batches(chunks, graphProperties.getBatchSize())) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (EmbeddingChunk chunk : batch) {
                Map<String, Object> row = chunk.toMap();
                row.put("foodIds", relatedFoodIds(chunk.text(), foodByName));
                rows.add(row);
            }

            session.executeWrite(tx -> {
                tx.run("""
                        UNWIND $rows AS row
                        MERGE (chunk:KnowledgeChunk {id: row.id})
                        SET chunk.text = row.text,
                            chunk.source = row.source,
                            chunk.metadata = row.metadata,
                            chunk.embedding = row.embedding
                        WITH chunk, row
                        UNWIND row.foodIds AS foodId
                        MATCH (food:Food {id: foodId})
                        MERGE (chunk)-[:DESCRIBES]->(food)
                        """, Map.of("rows", rows));
                return null;
            });
        }
    }

    private void writeSimilarityRelations(org.neo4j.driver.Session session, List<EmbeddingChunk> chunks) {
        List<Map<String, Object>> relations = new ArrayList<>();
        double threshold = graphProperties.getSimilarityThreshold();
        int topK = Math.max(1, graphProperties.getSimilarityTopK());

        for (EmbeddingChunk left : chunks) {
            List<SimilarityCandidate> candidates = new ArrayList<>();
            for (EmbeddingChunk right : chunks) {
                if (left.id().equals(right.id())) {
                    continue;
                }
                double score = cosine(left.embedding(), right.embedding());
                if (score >= threshold) {
                    candidates.add(new SimilarityCandidate(right.id(), score));
                }
            }

            candidates.stream()
                    .sorted(Comparator.comparingDouble(SimilarityCandidate::score).reversed())
                    .limit(topK)
                    .forEach(candidate -> relations.add(Map.of(
                            "sourceId", left.id(),
                            "targetId", candidate.targetId(),
                            "score", candidate.score()
                    )));
        }

        if (relations.isEmpty()) {
            return;
        }

        for (List<Map<String, Object>> batch : batches(relations, graphProperties.getBatchSize())) {
            session.executeWrite(tx -> {
                tx.run("""
                        UNWIND $rows AS row
                        MATCH (source:KnowledgeChunk {id: row.sourceId})
                        MATCH (target:KnowledgeChunk {id: row.targetId})
                        MERGE (source)-[rel:SIMILAR_TO]->(target)
                        SET rel.score = row.score
                        """, Map.of("rows", batch));
                return null;
            });
        }
    }

    private List<EmbeddingChunk> loadEmbeddingChunks() {
        String safeTable = safeTableName(pgTable);
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", pgHost, pgPort, pgDatabase);
        String sql = "SELECT embedding_id::text AS embedding_id, text, metadata::text AS metadata, embedding::text AS embedding FROM " + safeTable;

        List<EmbeddingChunk> chunks = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, pgUser, pgPassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("embedding_id");
                String text = cleanText(rs.getString("text"));
                String metadata = rs.getString("metadata");
                List<Double> embedding = parseVector(rs.getString("embedding"));

                if (id == null || id.isBlank() || embedding.isEmpty()) {
                    continue;
                }
                chunks.add(new EmbeddingChunk(id, text, metadata, sourceFromMetadata(metadata), embedding));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read pgvector table: " + safeTable, ex);
        }
        return chunks;
    }

    private List<FoodDocument> loadFoodDocuments() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<FoodDocument> foods = new ArrayList<>();

        try {
            Resource[] resources = resolver.getResources("classpath*:content/*.json");
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    JsonNode root = objectMapper.readTree(inputStream);
                    if (!root.isArray()) {
                        continue;
                    }
                    for (JsonNode node : root) {
                        foods.add(toFoodDocument(node));
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load knowledge graph food documents", ex);
        }

        return foods;
    }

    private FoodDocument toFoodDocument(JsonNode node) {
        String id = node.path("id").asText();
        String name = node.path("name").asText();
        String category = node.path("category").asText();
        String textForEmbedding = node.path("text_for_embedding").asText();

        Map<String, Object> nutrition = objectMapper.convertValue(
                node.path("nutrition_per_100g"),
                new TypeReference<>() {
                }
        );
        List<String> features = objectMapper.convertValue(
                node.path("features"),
                new TypeReference<>() {
                }
        );

        return new FoodDocument(
                id,
                name,
                category,
                nutrition == null ? Map.of() : nutrition,
                features == null ? List.of() : features,
                textForEmbedding
        );
    }

    private List<String> relatedFoodIds(String text, Map<String, FoodDocument> foodByName) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> foodIds = new HashSet<>();
        for (FoodDocument food : foodByName.values()) {
            if (text.contains(food.name()) || text.contains(food.textForEmbedding())) {
                foodIds.add(food.id());
            }
        }
        return new ArrayList<>(foodIds);
    }

    private SessionConfig sessionConfig() {
        String database = graphProperties.getDatabase();
        if (database == null || database.isBlank()) {
            return SessionConfig.defaultConfig();
        }
        return SessionConfig.builder().withDatabase(database).build();
    }

    private String safeTableName(String table) {
        String safeTable = table == null ? "" : table.trim();
        if (!safeTable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid pgvector table name: " + table);
        }
        return safeTable;
    }

    private List<Double> parseVector(String vectorText) {
        if (vectorText == null || vectorText.isBlank()) {
            return List.of();
        }

        String normalized = vectorText.trim()
                .replace("[", "")
                .replace("]", "");
        if (normalized.isBlank()) {
            return List.of();
        }

        List<Double> vector = new ArrayList<>();
        for (String part : normalized.split(",")) {
            String value = part.trim();
            if (!value.isBlank()) {
                vector.add(Double.parseDouble(value));
            }
        }
        return vector;
    }

    private String cleanText(String rawText) {
        if (rawText == null) {
            return "";
        }
        Matcher matcher = TEXT_FOR_EMBEDDING_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .trim();
        }
        return rawText.trim();
    }

    private String sourceFromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return "consultant_embedding";
        }
        try {
            JsonNode node = objectMapper.readTree(metadata);
            for (String key : List.of("source", "file_name", "fileName", "absolute_file_path", "path", "url")) {
                String value = node.path(key).asText();
                if (value != null && !value.isBlank()) {
                    return cleanSource(value);
                }
            }
        } catch (Exception ignored) {
            return "consultant_embedding";
        }
        return "consultant_embedding";
    }

    private String cleanSource(String source) {
        String normalized = source.replace("\\", "/");
        int slash = normalized.lastIndexOf("/");
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.replaceAll("\\.(json|md|txt|pdf)$", "");
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0D;
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < left.size(); i++) {
            double leftValue = left.get(i);
            double rightValue = right.get(i);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private <T> List<List<T>> batches(List<T> rows, int batchSize) {
        if (rows.isEmpty()) {
            return List.of();
        }

        int size = Math.max(1, batchSize);
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += size) {
            batches.add(rows.subList(i, Math.min(i + size, rows.size())));
        }
        return batches;
    }

    private record SimilarityCandidate(String targetId, double score) {
    }

    public record SyncResult(int foodNodes,
                             int embeddingChunks,
                             boolean similarityRelationsEnabled,
                             String warning) {
    }

    private record EmbeddingChunk(String id,
                                  String text,
                                  String metadata,
                                  String source,
                                  List<Double> embedding) {

        private Map<String, Object> toMap() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", id);
            row.put("text", text);
            row.put("metadata", metadata == null ? "" : metadata);
            row.put("source", source);
            row.put("embedding", embedding);
            return row;
        }
    }

    private record FoodDocument(String id,
                                String name,
                                String category,
                                Map<String, Object> nutrition,
                                List<String> features,
                                String textForEmbedding) {

        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("name", name);
            row.put("category", category);
            row.put("nutrition", nutrition.toString());
            row.put("features", features);
            row.put("textForEmbedding", textForEmbedding);
            row.put("nutrientRows", nutrientRows());
            return row;
        }

        private List<Map<String, Object>> nutrientRows() {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map.Entry<String, Object> entry : nutrition.entrySet()) {
                String name = entry.getKey();
                Map<String, Object> row = new HashMap<>();
                row.put("name", name);
                row.put("amount", entry.getValue());
                row.put("unit", nutrientUnit(name));
                rows.add(row);
            }
            return rows;
        }

        private String nutrientUnit(String nutrientName) {
            String lower = nutrientName.toLowerCase(Locale.ROOT);
            if (lower.endsWith("_mg")) {
                return "mg";
            }
            if (lower.endsWith("_g")) {
                return "g";
            }
            if (lower.equals("kcal")) {
                return "kcal";
            }
            return "";
        }
    }
}
