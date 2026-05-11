package com.zyt.consultant.rag;

import dev.langchain4j.data.document.Metadata;

public record RetrievedTextSegment(
        String text,
        String source,
        Metadata metadata,
        double score
) {
    public RetrievedTextSegment {
        metadata = metadata == null ? new Metadata() : metadata.copy();
    }
}
