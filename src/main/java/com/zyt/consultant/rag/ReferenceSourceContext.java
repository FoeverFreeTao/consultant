package com.zyt.consultant.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ReferenceSourceContext {

    private static final ThreadLocal<Set<String>> SOURCES = ThreadLocal.withInitial(LinkedHashSet::new);

    private ReferenceSourceContext() {
    }

    public static void clear() {
        SOURCES.remove();
    }

    public static void addSource(String source) {
        if (source == null) {
            return;
        }
        String cleaned = source.trim();
        if (!cleaned.isEmpty()) {
            SOURCES.get().add(cleaned);
        }
    }

    public static void addSources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        for (String source : sources) {
            addSource(source);
        }
    }

    public static List<String> snapshot() {
        return new ArrayList<>(SOURCES.get());
    }
}
