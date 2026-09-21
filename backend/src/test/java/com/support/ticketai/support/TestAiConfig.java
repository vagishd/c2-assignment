package com.support.ticketai.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Provides deterministic, offline test doubles for the AI model beans so integration tests load the
 * full context WITHOUT contacting Ollama or OpenAI (see test-strategy.md — no live model calls in CI).
 *
 * <p>The embedding model returns a fixed-dimension vector so SimpleVectorStore can add/search
 * without a real model. The chat model is a mock; tests that assert grounding do so at the unit
 * level in AskServiceTest.
 */
@TestConfiguration
public class TestAiConfig {

    private static final int EMBEDDING_DIM = 8;

    @Bean
    @Primary
    EmbeddingModel testEmbeddingModel() {
        // A tiny deterministic embedding model: a fixed non-null vector for anything.
        // Enough for SimpleVectorStore to add and search offline; grounding is unit-tested separately.
        return new EmbeddingModel() {
            private float[] fixedVector() {
                float[] vector = new float[EMBEDDING_DIM];
                for (int i = 0; i < EMBEDDING_DIM; i++) {
                    vector[i] = 0.1f * (i + 1);
                }
                return vector;
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    embeddings.add(new Embedding(fixedVector(), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return fixedVector();
            }

            @Override
            public int dimensions() {
                return EMBEDDING_DIM;
            }
        };
    }

    @Bean
    @Primary
    ChatModel testChatModel() {
        return mock(ChatModel.class);
    }
}
