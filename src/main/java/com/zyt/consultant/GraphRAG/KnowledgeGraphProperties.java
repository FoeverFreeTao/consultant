package com.zyt.consultant.GraphRAG;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.knowledge-graph.neo4j")
public class KnowledgeGraphProperties {

    private boolean enabled = false;
    private boolean autoSync = false;
    private String uri = "bolt://127.0.0.1:7688";
    private String user = "neo4j";
    private String password = "12345678";
    private String database = "neo4j";
    private int batchSize = 100;
    private int vectorDimension = 1024;
    private boolean createSimilarityRelations = true;
    private double similarityThreshold = 0.82;
    private int similarityTopK = 3;
    private boolean ragSupplementEnabled = true;
    private int ragMaxFoodMatches = 4;
    private int ragMaxRelations = 4;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoSync() {
        return autoSync;
    }

    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }

    public void setVectorDimension(int vectorDimension) {
        this.vectorDimension = vectorDimension;
    }

    public boolean isCreateSimilarityRelations() {
        return createSimilarityRelations;
    }

    public void setCreateSimilarityRelations(boolean createSimilarityRelations) {
        this.createSimilarityRelations = createSimilarityRelations;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getSimilarityTopK() {
        return similarityTopK;
    }

    public void setSimilarityTopK(int similarityTopK) {
        this.similarityTopK = similarityTopK;
    }

    public boolean isRagSupplementEnabled() {
        return ragSupplementEnabled;
    }

    public void setRagSupplementEnabled(boolean ragSupplementEnabled) {
        this.ragSupplementEnabled = ragSupplementEnabled;
    }

    public int getRagMaxFoodMatches() {
        return ragMaxFoodMatches;
    }

    public void setRagMaxFoodMatches(int ragMaxFoodMatches) {
        this.ragMaxFoodMatches = ragMaxFoodMatches;
    }

    public int getRagMaxRelations() {
        return ragMaxRelations;
    }

    public void setRagMaxRelations(int ragMaxRelations) {
        this.ragMaxRelations = ragMaxRelations;
    }
}
