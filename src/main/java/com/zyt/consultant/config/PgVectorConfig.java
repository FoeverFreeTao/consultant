package com.zyt.consultant.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Configuration
public class PgVectorConfig {
    private static final Logger log = LoggerFactory.getLogger(PgVectorConfig.class);

    @Value("${app.vector-store.pgvector.host:127.0.0.1}")
    private String host;

    @Value("${app.vector-store.pgvector.port:5432}")
    private Integer port;

    @Value("${app.vector-store.pgvector.user:postgres}")
    private String user;

    @Value("${app.vector-store.pgvector.password:123456}")
    private String password;

    @Value("${app.vector-store.pgvector.database:vector_db}")
    private String database;

    @Value("${app.vector-store.pgvector.table:consultant_embedding}")
    private String table;

    @Value("${app.vector-store.pgvector.dimension:1024}")
    private Integer dimension;

    @Value("${app.vector-store.pgvector.create-table:true}")
    private Boolean createTable;

    @Value("${app.vector-store.pgvector.drop-table-first:false}")
    private Boolean dropTableFirst;

    @Value("${app.vector-store.pgvector.use-index:true}")
    private Boolean useIndex;

    @Value("${app.vector-store.pgvector.index-list-size:100}")
    private Integer indexListSize;

    @Value("${app.vector-store.pgvector.skip-create-vector-extension:false}")
    private Boolean skipCreateVectorExtension;

    @Value("${app.vector-store.pgvector.auto-ingest:true}")
    private Boolean autoIngest;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        EmbeddingStore<TextSegment> store = PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .database(database)
                .table(table)
                .dimension(dimension)
                .createTable(createTable)
                .dropTableFirst(dropTableFirst)
                .useIndex(useIndex)
                .indexListSize(indexListSize)
                .skipCreateVectorExtension(skipCreateVectorExtension)
                .build();
        if (Boolean.TRUE.equals(autoIngest) && isTableEmpty()) {
            ingestContent(store, embeddingModel);
        }
        return store;
    }

    private void ingestContent(EmbeddingStore<TextSegment> store, EmbeddingModel embeddingModel) {
        List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("content");
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
        log.info("pgvector ingest finished. documents={}", documents.size());
    }

    private boolean isTableEmpty() {
        String safeTable = table == null ? "" : table.trim();
        if (!safeTable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid pgvector table name: " + safeTable);
        }

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        String sql = "SELECT COUNT(1) FROM " + safeTable;
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1) == 0L;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Failed to detect pgvector table row count, fallback to ingest. table={}", safeTable, ex);
            return true;
        }
    }
}
