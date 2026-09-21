package com.support.ticketai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retrieval-augmented-generation tuning parameters.
 * Bound from the {@code rag.*} config so top-K and the similarity threshold are configurable,
 * never hardcoded (FR-20).
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        int topK,
        double similarityThreshold,
        String provider
) {
    public RagProperties {
        if (topK <= 0) {
            topK = 4;
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            similarityThreshold = 0.5;
        }
        if (provider == null || provider.isBlank()) {
            provider = "ollama";
        }
    }
}
