package com.support.ticketai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the in-memory {@link SimpleVectorStore} to the embedding model chosen by the configured
 * provider (Ollama by default, OpenAI when the 'openai' profile is active). See architecture.md.
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    /**
     * Selects the embedding model. Both the Ollama and OpenAI starters may put an
     * {@link EmbeddingModel} on the context; we pick deterministically based on {@code rag.provider}
     * so there is never an ambiguous choice.
     */
    @Bean
    public VectorStore vectorStore(ObjectProvider<EmbeddingModel> embeddingModels,
                                   RagProperties ragProperties) {
        EmbeddingModel embeddingModel = embeddingModels.stream()
                .filter(model -> matchesProvider(model, ragProperties.provider()))
                .findFirst()
                .orElseGet(() -> {
                    EmbeddingModel fallback = embeddingModels.getObject();
                    log.warn("No embedding model matched provider '{}'; using {}",
                            ragProperties.provider(), fallback.getClass().getSimpleName());
                    return fallback;
                });

        log.info("Vector store using embedding model: {}", embeddingModel.getClass().getSimpleName());
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    private boolean matchesProvider(EmbeddingModel model, String provider) {
        String name = model.getClass().getSimpleName().toLowerCase();
        return name.contains(provider.toLowerCase());
    }
}
