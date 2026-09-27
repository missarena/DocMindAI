package com.example.docmind.service;

import com.example.docmind.Exception.DocumentProcessingException;
import com.example.docmind.config.AppProperties;
import com.example.docmind.entity.DocumentMetadata;
import com.example.docmind.entity.DocumentStatus;
import com.example.docmind.repository.DocumentMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {
    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final DocumentMetadataRepo documentMetadataRepo;
    private final AppProperties appProperties;


    public int ingest(DocumentMetadata metadata, List<Document> parsedDocs) {

        log.info(
                "Ingesting document [id={}, name={}, pages={}]",
                metadata.getId(),
                metadata.getFilename(),
                metadata.getFileSize()
        );

        try {

            metadata.setStatus(DocumentStatus.PROCESSING);
            metadata.setTotalPages(parsedDocs.size());

            // 1. Text chunking
            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                    .withChunkSize(appProperties.getRag().getChunkSize())
                    .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
                    .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                    .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = tokenTextSplitter.apply(parsedDocs);

            if (chunks.isEmpty()) {
                throw new DocumentProcessingException(
                        "Document appears to be empty or unscannable"
                );
            }

            // 2. Metadata enrichment
            List<Document> enrichedChunks = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {

                Document chunk = chunks.get(i);

                Map<String, Object> enrichedMetadata =
                        new HashMap<>(chunk.getMetadata());

                enrichedMetadata.put(
                        "documentId",
                        metadata.getId().toString()
                );

                enrichedMetadata.put(
                        "fileName",
                        metadata.getFilename()
                );

                enrichedMetadata.put(
                        "contentType",
                        metadata.getContentType()
                );

                enrichedMetadata.put(
                        "chunkIndex",
                        i
                );

                Object pageNumber =
                        chunk.getMetadata().get("page_number");

                if (pageNumber == null) {
                    pageNumber =
                            chunk.getMetadata().get("pageNumber");
                }

                if (pageNumber != null) {
                    enrichedMetadata.put(
                            "pageNumber",
                            pageNumber
                    );
                }

                Document enrichedDoc =
                        new Document(
                                chunk.getText(),
                                enrichedMetadata
                        );

                enrichedChunks.add(enrichedDoc);
            }

            // 3. Store embeddings in pgvector
            log.info(
                    "Writing {} vector chunks to PgVectorStore for document: {}",
                    enrichedChunks.size(),
                    metadata.getFilename()
            );

            vectorStore.add(enrichedChunks);

            // 4. Successfully indexed
            metadata.setStatus(DocumentStatus.INDEXED);
            metadata.setTotalChunks(enrichedChunks.size());
            metadata.setErrorMessage(null);

            documentMetadataRepo.save(metadata);

            log.info(
                    "Successfully indexed document [id={}, name={}, chunks={}]",
                    metadata.getId(),
                    metadata.getFilename(),
                    enrichedChunks.size()
            );

            return enrichedChunks.size();

        } catch (DocumentProcessingException ex) {

            log.error(
                    "Failed to process document: {}",
                    metadata.getFilename(),
                    ex
            );

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Failed to ingest document into vector store: {}",
                    metadata.getFilename(),
                    ex
            );

            throw new DocumentProcessingException(
                    "Failed to index document: " + ex.getMessage(),
                    ex
            );
        }
    }
}