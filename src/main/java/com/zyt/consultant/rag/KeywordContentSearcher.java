package com.zyt.consultant.rag;

import java.util.List;

public interface KeywordContentSearcher {

    List<RetrievedTextSegment> search(String queryText, int maxResults);
}
