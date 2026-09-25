package com.support.ticketai.rag;

import com.support.ticketai.config.RagProperties;
import com.support.ticketai.dto.AskResponse;
import com.support.ticketai.dto.TicketSource;
import com.support.ticketai.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single retrieve-then-generate flow, grounded strictly in ticket data (see rag-api-contract.md).
 *
 * <p>Guardrails:
 * <ul>
 *   <li>If similarity search returns nothing above the configured threshold, we return the honest
 *       no-match response WITHOUT calling the LLM — the strongest anti-hallucination guarantee.</li>
 *   <li>The system prompt constrains the model to answer only from the provided ticket context and
 *       to cite ticket IDs. It is a constant, so the static instructions are stable/cacheable.</li>
 * </ul>
 */
@Slf4j
@Service
public class AskService {

    private static final String SYSTEM_PROMPT = """
            You are a support assistant. Answer the user's question using ONLY the ticket context
            provided below. Do not use any outside or general knowledge.
            Cite the specific ticket IDs (e.g. TKT-1001) that support your answer.
            If the context does not contain the answer, reply exactly:
            "%s"
            Do not guess or fabricate.

            Ticket context:
            {context}
            """.formatted(AskResponse.NO_MATCH_MESSAGE);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public AskService(ChatClient.Builder chatClientBuilder,
                      VectorStore vectorStore,
                      RagProperties ragProperties) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    public AskResponse ask(String question) {
        List<Document> matches;
        try {
            matches = retrieve(question);
        } catch (Exception ex) {
            // Embedding the question failed — most likely the model service is offline.
            log.warn("Retrieval failed — embedding model unavailable ({})", ex.getMessage());
            throw new AiUnavailableException(ex);
        }

        if (matches.isEmpty()) {
            log.info("No tickets above similarity threshold for question; returning no-match");
            return AskResponse.noMatch();
        }

        String context = buildContext(matches);
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(system -> system.text(SYSTEM_PROMPT).param("context", context))
                    .user(question)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Chat generation failed — model unavailable ({})", ex.getMessage());
            throw new AiUnavailableException(ex);
        }

        // Reconcile: if the model emitted the no-match sentence despite having context, treat it as a
        // genuine no-match rather than returning a contradictory answer with populated sources.
        if (answer == null || answer.isBlank() || isNoMatch(answer)) {
            log.info("Model produced no grounded answer from {} candidate(s); returning no-match",
                    matches.size());
            return AskResponse.noMatch();
        }

        List<TicketSource> sources = extractSources(matches);
        log.info("Answered question grounded in {} ticket(s)", sources.size());
        return new AskResponse(answer.trim(), sources, true);
    }

    private boolean isNoMatch(String answer) {
        String normalized = answer.toLowerCase().replaceAll("[\"'.\\s]+", " ").trim();
        String target = AskResponse.NO_MATCH_MESSAGE.toLowerCase().replaceAll("[\"'.\\s]+", " ").trim();
        return normalized.contains(target);
    }

    private List<Document> retrieve(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(ragProperties.topK())
                .similarityThreshold(ragProperties.similarityThreshold())
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        return results == null ? List.of() : results;
    }

    private String buildContext(List<Document> matches) {
        StringBuilder sb = new StringBuilder();
        for (Document doc : matches) {
            sb.append(doc.getText()).append("\n---\n");
        }
        return sb.toString();
    }

    /** One source per distinct ticket, preserving retrieval order. */
    private List<TicketSource> extractSources(List<Document> matches) {
        Map<String, TicketSource> byTicket = new LinkedHashMap<>();
        for (Document doc : matches) {
            Map<String, Object> meta = doc.getMetadata();
            String ticketId = asString(meta.get(KnowledgeDocumentFactory.META_TICKET_ID));
            if (ticketId == null || byTicket.containsKey(ticketId)) {
                continue;
            }
            byTicket.put(ticketId, new TicketSource(
                    ticketId,
                    asString(meta.get(KnowledgeDocumentFactory.META_TITLE)),
                    asString(meta.get(KnowledgeDocumentFactory.META_STATUS))
            ));
        }
        return List.copyOf(byTicket.values());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
