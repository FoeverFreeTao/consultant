package com.zyt.consultant.config;

import com.zyt.consultant.GraphRAG.GraphRelationRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.consultant.rag.CrossEncoderReranker;
import com.zyt.consultant.rag.DashScopeCrossEncoderReranker;
import com.zyt.consultant.rag.ElasticsearchKeywordContentSearcher;
import com.zyt.consultant.rag.HybridContentRetriever;
import com.zyt.consultant.rag.KeywordContentSearcher;
import com.zyt.consultant.rag.ParallelRagContentRetriever;
import com.zyt.consultant.rag.RagRerankProperties;
import com.zyt.consultant.rag.RagSearchProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableConfigurationProperties({RagSearchProperties.class, RagRerankProperties.class})
public class CommonConfig {

    @Autowired
    private ChatMemoryStore redisChatMemoryStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private ObjectProvider<GraphRelationRetriever> graphRelationRetrieverProvider;

    @Value("${app.rag.parallel.graph-timeout-ms:1200}")
    private long graphRagTimeoutMs;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }
    //之前redis构建向量数据库操作对象
//    @Bean
//    public EmbeddingStore  embeddingStore(){
//        //1。加载文档进内存
//        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
//        //pdf类
////        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content",new ApachePdfBoxDocumentParser());
////        List<Document> documents = FileSystemDocumentLoader.loadDocuments("c:content");
//        //2.构建向量数据库操作对象,操作的是内存版本的向量数据库
    ////        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
//        //构建文档分割器对象
//        DocumentSplitter ds = DocumentSplitters.recursive(500,100);
//        //3.构建一个EmbeddingStoreIngestor对象，完成文本数据切割，向量化，存储
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .embeddingStore(redisEmbeddingStore)
//                .documentSplitter(ds)
//                .embeddingModel(embeddingModel)
//                .build();
//        ingestor.ingest(documents);
//        return redisEmbeddingStore;
//    }
    @Bean
    public KeywordContentSearcher keywordContentSearcher(RagSearchProperties searchProperties,
                                                         ObjectMapper objectMapper) {
        return new ElasticsearchKeywordContentSearcher(searchProperties, loadKeywordSegments(), objectMapper);
    }

    @Bean
    public CrossEncoderReranker crossEncoderReranker(RagRerankProperties rerankProperties) {
        return new DashScopeCrossEncoderReranker(rerankProperties);
    }

    @Bean
    public ContentRetriever contentRetriever(KeywordContentSearcher keywordContentSearcher,
                                             CrossEncoderReranker crossEncoderReranker,
                                             RagRerankProperties rerankProperties) {
        ContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .minScore(0.35)
                .maxResults(Math.max(8, rerankProperties.getCandidateLimit() / 2))
                .embeddingModel(embeddingModel)
                .build();

        ContentRetriever traditionalRetriever = new HybridContentRetriever(
                vectorRetriever,
                keywordContentSearcher,
                crossEncoderReranker,
                Math.max(8, rerankProperties.getCandidateLimit()),
                rerankProperties.getTopN(),
                rerankProperties.getCandidateLimit(),
                0.7,
                0.3
        );
        GraphRelationRetriever graphRelationRetriever = graphRelationRetrieverProvider.getIfAvailable();
        if (graphRelationRetriever == null) {
            return traditionalRetriever;
        }
        return new ParallelRagContentRetriever(
                traditionalRetriever,
                graphRelationRetriever,
                graphRagTimeoutMs
        );
    }

    private List<TextSegment> loadKeywordSegments() {
        try {
            List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("content");
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
            return splitter.splitAll(documents);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
