package com.zyt.consultant.GraphRAG;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge-graph")
@ConditionalOnProperty(prefix = "app.knowledge-graph.neo4j", name = "enabled", havingValue = "true")
public class KnowledgeGraphController {

    private final KnowledgeGraphSyncService knowledgeGraphSyncService;

    public KnowledgeGraphController(KnowledgeGraphSyncService knowledgeGraphSyncService) {
        this.knowledgeGraphSyncService = knowledgeGraphSyncService;
    }

    @PostMapping("/sync")
    public KnowledgeGraphSyncService.SyncResult sync() {
        return knowledgeGraphSyncService.sync();
    }

    @PostMapping("/sync-foods")
    public KnowledgeGraphSyncService.SyncResult syncFoods() {
        return knowledgeGraphSyncService.syncFoods();
    }
}
