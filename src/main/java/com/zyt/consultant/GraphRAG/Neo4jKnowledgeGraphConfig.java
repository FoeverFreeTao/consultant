package com.zyt.consultant.GraphRAG;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeGraphProperties.class)
public class Neo4jKnowledgeGraphConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.knowledge-graph.neo4j", name = "enabled", havingValue = "true")
    public Driver neo4jDriver(KnowledgeGraphProperties properties) {
        return GraphDatabase.driver(
                properties.getUri(),
                AuthTokens.basic(properties.getUser(), properties.getPassword())
        );
    }
}
