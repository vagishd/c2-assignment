package com.support.ticketai.rag;

import com.support.ticketai.domain.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the in-memory vector store in sync with ticket data.
 *
 * <p>Re-ingestion is idempotent: we track the document ids produced for each ticketRef, delete
 * them, then add the freshly built document(s). This prevents duplicates and stale content when a
 * ticket is updated, commented on, or transitioned (FR-19). See rag-ingestion.md.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreIngestionService implements TicketIngestionService {

    // A ticket larger than this (characters) is split into token-bounded chunks; otherwise it is
    // kept as a single self-contained document so citation maps one chunk to one ticket.
    private static final int OVERSIZED_TICKET_CHARS = 4000;

    private final VectorStore vectorStore;
    private final KnowledgeDocumentFactory documentFactory;
    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    // ticketRef -> ids of the documents currently stored for it, so we can remove them precisely.
    private final Map<String, List<String>> documentIdsByTicket = new ConcurrentHashMap<>();

    @Override
    public void ingest(Ticket ticket) {
        String ticketRef = ticket.getTicketRef();
        remove(ticketRef);

        Document document = documentFactory.build(ticket);
        List<Document> chunks = document.getText().length() > OVERSIZED_TICKET_CHARS
                ? tokenTextSplitter.split(document)
                : List.of(document);

        vectorStore.add(chunks);
        documentIdsByTicket.put(ticketRef, chunks.stream().map(Document::getId).toList());
        log.debug("Ingested ticket {} as {} chunk(s)", ticketRef, chunks.size());
    }

    @Override
    public void remove(String ticketRef) {
        List<String> existingIds = documentIdsByTicket.remove(ticketRef);
        if (existingIds != null && !existingIds.isEmpty()) {
            vectorStore.delete(existingIds);
        }
    }
}
