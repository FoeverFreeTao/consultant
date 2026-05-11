package com.zyt.consultant.GraphRAG;

import com.zyt.consultant.rag.ReferenceSourceContext;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.query.Query;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnBean(Driver.class)
@ConditionalOnProperty(prefix = "app.knowledge-graph.neo4j", name = "rag-supplement-enabled", havingValue = "true", matchIfMissing = true)
public class GraphRelationRetriever {

    private static final Logger log = LoggerFactory.getLogger(GraphRelationRetriever.class);
    private static final String GRAPH_SOURCE = "Neo4j-GraphRAG";

    private final Driver driver;
    private final KnowledgeGraphProperties graphProperties;

    public GraphRelationRetriever(Driver driver, KnowledgeGraphProperties graphProperties) {
        this.driver = driver;
        this.graphProperties = graphProperties;
    }

    public List<Content> retrieve(Query query) {
        String normalizedQuery = normalize(query == null ? "" : query.text());
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        try (org.neo4j.driver.Session session = driver.session(sessionConfig())) {
            List<GraphFoodRelation> relations = loadDirectFoodRelations(session, normalizedQuery);
            if (relations.isEmpty()) {
                relations = loadAttributeFoodRelations(session, normalizedQuery);
            }
            if (relations.isEmpty()) {
                return List.of();
            }

            ReferenceSourceContext.addSource(GRAPH_SOURCE);
            TextSegment segment = TextSegment.from(toGraphContext(relations), Metadata.from("source", GRAPH_SOURCE));
            Map<ContentMetadata, Object> metadata = new LinkedHashMap<>();
            metadata.put(ContentMetadata.SCORE, 0.05D);
            metadata.put(ContentMetadata.RERANKED_SCORE, 0.05D);
            return List.of(Content.from(segment, metadata));
        } catch (Exception ex) {
            log.warn("GraphRAG supplement retrieve failed, fallback to traditional RAG only", ex);
            return List.of();
        }
    }

    private List<GraphFoodRelation> loadDirectFoodRelations(org.neo4j.driver.Session session, String normalizedQuery) {
        String cypher = """
                MATCH (food:Food)
                WHERE food.name IS NOT NULL AND (
                    $query CONTAINS toLower(food.name)
                    OR toLower(food.name) CONTAINS $query
                    OR toLower(coalesce(food.text, '')) CONTAINS $query
                )
                WITH food
                LIMIT $foodLimit
                OPTIONAL MATCH (food)-[:BELONGS_TO]->(category:Category)
                WITH food, collect(DISTINCT category.name) AS categories
                OPTIONAL MATCH (food)-[:HAS_FEATURE]->(feature:Feature)
                WITH food, categories, collect(DISTINCT feature.name)[0..6] AS features
                OPTIONAL MATCH (food)-[nutrientRel:HAS_NUTRIENT]->(nutrient:Nutrient)
                WITH food, categories, features,
                     collect(DISTINCT nutrient.name + '=' + toString(nutrientRel.amount) + coalesce(nutrientRel.unit, ''))[0..8] AS nutrients
                OPTIONAL MATCH (food)<-[:DESCRIBES]-(chunk:KnowledgeChunk)-[:SIMILAR_TO]->(:KnowledgeChunk)-[:DESCRIBES]->(similarFood:Food)
                WHERE similarFood.id <> food.id
                WITH food, categories, features, nutrients, collect(DISTINCT similarFood.name)[0..5] AS similarFoods
                RETURN food.name AS foodName,
                       food.category AS category,
                       food.text AS description,
                       categories AS categories,
                       features AS features,
                       nutrients AS nutrients,
                       similarFoods AS similarFoods
                LIMIT $relationLimit
                """;
        return runRelationQuery(session, cypher, normalizedQuery);
    }

    private List<GraphFoodRelation> loadAttributeFoodRelations(org.neo4j.driver.Session session, String normalizedQuery) {
        String cypher = """
                MATCH (food:Food)-[:BELONGS_TO|HAS_FEATURE|HAS_NUTRIENT]->(attribute)
                WHERE attribute.name IS NOT NULL AND (
                    $query CONTAINS toLower(attribute.name)
                    OR toLower(attribute.name) CONTAINS $query
                )
                WITH DISTINCT food
                LIMIT $foodLimit
                OPTIONAL MATCH (food)-[:BELONGS_TO]->(category:Category)
                WITH food, collect(DISTINCT category.name) AS categories
                OPTIONAL MATCH (food)-[:HAS_FEATURE]->(feature:Feature)
                WITH food, categories, collect(DISTINCT feature.name)[0..6] AS features
                OPTIONAL MATCH (food)-[nutrientRel:HAS_NUTRIENT]->(nutrient:Nutrient)
                WITH food, categories, features,
                     collect(DISTINCT nutrient.name + '=' + toString(nutrientRel.amount) + coalesce(nutrientRel.unit, ''))[0..8] AS nutrients
                OPTIONAL MATCH (food)<-[:DESCRIBES]-(chunk:KnowledgeChunk)-[:SIMILAR_TO]->(:KnowledgeChunk)-[:DESCRIBES]->(similarFood:Food)
                WHERE similarFood.id <> food.id
                WITH food, categories, features, nutrients, collect(DISTINCT similarFood.name)[0..5] AS similarFoods
                RETURN food.name AS foodName,
                       food.category AS category,
                       food.text AS description,
                       categories AS categories,
                       features AS features,
                       nutrients AS nutrients,
                       similarFoods AS similarFoods
                LIMIT $relationLimit
                """;
        return runRelationQuery(session, cypher, normalizedQuery);
    }

    private List<GraphFoodRelation> runRelationQuery(org.neo4j.driver.Session session, String cypher, String normalizedQuery) {
        Map<String, Object> params = Map.of(
                "query", normalizedQuery,
                "foodLimit", Math.max(1, graphProperties.getRagMaxFoodMatches()),
                "relationLimit", Math.max(1, graphProperties.getRagMaxRelations())
        );

        return session.executeRead(tx -> tx.run(cypher, params)
                .list(this::toGraphFoodRelation));
    }

    private GraphFoodRelation toGraphFoodRelation(Record record) {
        return new GraphFoodRelation(
                record.get("foodName").asString(""),
                record.get("category").asString(""),
                record.get("description").asString(""),
                listStrings(record.get("categories")),
                listStrings(record.get("features")),
                listStrings(record.get("nutrients")),
                listStrings(record.get("similarFoods"))
        );
    }

    private List<String> listStrings(Value value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (String item : value.asList(v -> v.isNull() ? "" : v.asString(""))) {
            if (item != null && !item.isBlank() && !strings.contains(item)) {
                strings.add(item.trim());
            }
        }
        return strings;
    }

    private String toGraphContext(List<GraphFoodRelation> relations) {
        StringBuilder sb = new StringBuilder();
        sb.append("GraphRAG relationship supplement. Use this only as supporting relation reasoning; summarize it naturally and never expose raw graph schema or Cypher.\n");
        sb.append("[Source:").append(GRAPH_SOURCE).append("]\n");
        sb.append("图谱关系补充：\n");
        for (GraphFoodRelation relation : relations) {
            sb.append("- 食物：").append(relation.foodName());
            appendPart(sb, "类别", firstNonBlank(relation.category(), String.join("、", relation.categories())));
            appendPart(sb, "特征", String.join("、", relation.features()));
            appendPart(sb, "主要营养关系", String.join("、", relation.nutrients()));
            appendPart(sb, "相似或关联食物", String.join("、", relation.similarFoods()));
            appendPart(sb, "说明", truncate(relation.description(), 120));
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private void appendPart(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append("；").append(label).append("：").append(value.trim());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank() || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private SessionConfig sessionConfig() {
        String database = graphProperties.getDatabase();
        if (database == null || database.isBlank()) {
            return SessionConfig.defaultConfig();
        }
        return SessionConfig.builder().withDatabase(database).build();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}\\p{L}\\p{Nd}]", "")
                .trim();
    }

    private record GraphFoodRelation(String foodName,
                                     String category,
                                     String description,
                                     List<String> categories,
                                     List<String> features,
                                     List<String> nutrients,
                                     List<String> similarFoods) {
    }
}
