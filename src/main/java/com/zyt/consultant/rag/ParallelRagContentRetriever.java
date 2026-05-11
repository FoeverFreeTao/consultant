package com.zyt.consultant.rag;

import com.zyt.consultant.GraphRAG.GraphRelationRetriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParallelRagContentRetriever implements ContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(ParallelRagContentRetriever.class);

    private final ContentRetriever traditionalRetriever;
    private final GraphRelationRetriever graphRelationRetriever;
    private final long graphTimeoutMs;

    public ParallelRagContentRetriever(ContentRetriever traditionalRetriever,
                                       GraphRelationRetriever graphRelationRetriever,
                                       long graphTimeoutMs) {
        this.traditionalRetriever = traditionalRetriever;
        this.graphRelationRetriever = graphRelationRetriever;
        this.graphTimeoutMs = graphTimeoutMs;
    }

    @Override
    public List<Content> retrieve(Query query) {
        CompletableFuture<List<Content>> traditionalFuture = CompletableFuture
                .supplyAsync(() -> safeRetrieveTraditional(query));
        CompletableFuture<List<Content>> graphFuture = CompletableFuture
                .supplyAsync(() -> safeRetrieveGraph(query))
                .completeOnTimeout(Collections.emptyList(), Math.max(100L, graphTimeoutMs), TimeUnit.MILLISECONDS);

        List<Content> traditionalContents = traditionalFuture.join();
        List<Content> graphContents = graphFuture.join();

        List<Content> merged = new ArrayList<>(traditionalContents.size() + graphContents.size());
        merged.addAll(traditionalContents);
        merged.addAll(graphContents);
        collectReferenceSources(merged);
        return merged;
    }

    private List<Content> safeRetrieveTraditional(Query query) {
        try {
            List<Content> contents = traditionalRetriever.retrieve(query);
            return contents == null ? Collections.emptyList() : contents;
        } catch (Exception ex) {
            log.warn("Traditional RAG retrieve failed", ex);
            return Collections.emptyList();
        }
    }

    private List<Content> safeRetrieveGraph(Query query) {
        if (graphRelationRetriever == null) {
            return Collections.emptyList();
        }
        try {
            List<Content> contents = graphRelationRetriever.retrieve(query);
            return contents == null ? Collections.emptyList() : contents;
        } catch (Exception ex) {
            log.warn("GraphRAG supplement retrieve failed", ex);
            return Collections.emptyList();
        }
    }

    private void collectReferenceSources(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (Content content : contents) {
            if (content == null || content.textSegment() == null) {
                continue;
            }
            TextSegment segment = content.textSegment();
            String source = segment.metadata().getString("source");
            ReferenceSourceContext.addSource(source);
        }
    }
}
