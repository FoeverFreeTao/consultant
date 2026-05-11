package com.zyt.consultant.rag;

import java.util.List;

public interface CrossEncoderReranker {

    List<RerankScore> rerank(String queryText, List<String> documents, int topN);
}
