package com.support.ticketai.rag;

import com.support.ticketai.config.RagProperties;
import com.support.ticketai.dto.AskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Grounding behavior of the assistant, tested deterministically with a mocked vector store and
 * chat client (see evaluation-strategy.md). We assert properties, not exact generated text.
 */
class AskServiceTest {

    private VectorStore vectorStore;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private AskService askService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        RagProperties properties = new RagProperties(4, 0.5, "ollama");
        askService = new AskService(chatClientBuilder, vectorStore, properties);
    }

    @Test
    void ask_noMatchesAboveThreshold_returnsHonestNoMatch_andNeverCallsLlm() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        AskResponse response = askService.ask("Something completely unrelated to any ticket");

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        assertThat(response.answer()).contains("No relevant tickets");
        // The strongest anti-hallucination guarantee: the LLM is not consulted at all.
        verifyNoInteractions(chatClient);
    }

    @Test
    void ask_withRetrievedTickets_returnsGroundedAnswerCitingThoseTickets() {
        Document doc = new Document(
                "[TKT-1001] Payment failed at checkout\nResolution: renewed certificate.",
                Map.of(
                        KnowledgeDocumentFactory.META_TICKET_ID, "TKT-1001",
                        KnowledgeDocumentFactory.META_TITLE, "Payment failed at checkout",
                        KnowledgeDocumentFactory.META_STATUS, "RESOLVED"
                ));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        stubChatClientResponse("Payment failures were due to an expired certificate (TKT-1001).");

        AskResponse response = askService.ask("Have we seen payment failures before?");

        assertThat(response.grounded()).isTrue();
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).ticketId()).isEqualTo("TKT-1001");
        assertThat(response.sources().get(0).status()).isEqualTo("RESOLVED");
        assertThat(response.answer()).isNotBlank();
    }

    @Test
    void ask_duplicateTicketChunks_areCitedOnce() {
        Document chunk1 = new Document("[TKT-1001] part A",
                Map.of(KnowledgeDocumentFactory.META_TICKET_ID, "TKT-1001",
                        KnowledgeDocumentFactory.META_TITLE, "Payment failed",
                        KnowledgeDocumentFactory.META_STATUS, "RESOLVED"));
        Document chunk2 = new Document("[TKT-1001] part B",
                Map.of(KnowledgeDocumentFactory.META_TICKET_ID, "TKT-1001",
                        KnowledgeDocumentFactory.META_TITLE, "Payment failed",
                        KnowledgeDocumentFactory.META_STATUS, "RESOLVED"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk1, chunk2));
        stubChatClientResponse("See TKT-1001.");

        AskResponse response = askService.ask("payment");

        assertThat(response.sources()).hasSize(1);
    }

    // Sets up the fluent ChatClient call chain to return the given content.
    private void stubChatClientResponse(String content) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, org.mockito.Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(content);
    }
}
