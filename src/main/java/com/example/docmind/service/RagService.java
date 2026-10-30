package com.example.docmind.service;

import com.example.docmind.config.AppProperties;
import com.example.docmind.dto.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.geometry.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {
    private static final Logger log= LoggerFactory.getLogger(RagService.class);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final AppProperties appProperties;




    public ChatResponseDto askQuestion(ChatRequestDto request) {

        long startTime = System.currentTimeMillis();

        // Generate an ID if this is a new conversation
        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        log.info("Processing query: '{}', scoped documentId: {}",
                request.getQuestion(), request.getDocumentId());

        List<Document> similarDocuments = retrieveRelevantDocuments(
                request.getQuestion(),
                request.getDocumentId(),
                request.getTopK(),
                request.getMinSimilarity()
        );

        List<CitationDto> citationDtos = similarDocuments.stream()
                .map(this::mapToCitation)
                .toList();

        // Build document context
        String contextText = buildContextString(similarDocuments);

        // Include both document context and user question
        String userPrompt = buildPrompt(
                request.getQuestion(),
                contextText
        );

        log.info("Retrieved {} relevant chunks. Context length: {}",
                similarDocuments.size(), contextText.length());

        // Send the complete prompt to the LLM
        String answer = chatClient
                .prompt()
                .user(userPrompt)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId
                ))
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        return ChatResponseDto.builder()
                .answer(answer)
                .conversationId(conversationId)
                .citations(citationDtos)
                .responseTimeMs(responseTime)
                .build();
    }

//    private void ensureConversationExists(String conversationId, User user, @NotBlank(message = "Question cannot be empty") String question) {
//
//        conversationRepository.findByIdAndUser(conversationId, user)
//                .orElseGet(() -> {
//                    String title = question.length() > 50 ? question.substring(0, 47) + "....." : question;
//                    return conversationRepository.save(
//                            Conversation.builder().id(conversationId)
//                                    .title(title)
//                                    .user(user)
//                                    .createdAt(LocalDateTime.now())
//                                    .updatedAt(LocalDateTime.now())
//                                    .build()
//                    );
//                });
//
//
//    }

    //this streams
    public Flux<String> streamQuestionAnswer(ChatRequestDto requestDto) {
        log.info("Streaming query: '{}'", requestDto.getQuestion());
//        String conversationId = requestDto.getConversationId() != null ? requestDto.getConversationId() : UUID.randomUUID().toString();
//        requestDto.setConversationId(conversationId);
//        ensureConversationExists(conversationId, requestDto.getQuestion());

        List<Document> relevantDocuments = retrieveRelevantDocuments(
                requestDto.getQuestion(),
                requestDto.getDocumentId(),
                requestDto.getTopK(),
                requestDto.getMinSimilarity()

        );
        String contextText = buildContextString(relevantDocuments);
       String userPrompt = buildPrompt(requestDto.getQuestion(), contextText);
        return chatClient.prompt()
                //.system(s-> s.param("doc_context",contextText))
                .user(userPrompt)
                //.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, requestDto.getConversationId()))
                .stream()
                .content();
                //.concatWith(Flux.just("[DONE]"));


    }

    private String buildPrompt(@NotBlank(message = "Question cannot be empty") String question, String contextText) {

        if (contextText != null && !contextText.isBlank()) {
            return String.format("""
                           Document Context:
                           ---------------------
                           %s
                           ---------------------                                        
                           User Message / Question: %s             
                           Instructions:
                           - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
                           - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
                    
                    """, contextText, question);
        } else {
            return String.format("""
                       User Message / Question:
                       %s                         
                       Instructions:
                          - Respond helpfully, accurately, and conversationally to the user's message using your broad knowledge base.
                    
                    """, question);
        }


    }


    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }

        return similarDocuments.stream().map(doc -> {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
            return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));

    }




    public SearchResultDto searchSimilarChunks(SearchRequestDto request) {


        java.util.List<Document> matchedDocs = retrieveRelevantDocuments(request.getQuery(), request.getDocumentId(), request.getTopK(), request.getSimilaritySearch());

        List<CitationDto> citations = matchedDocs.stream().map(this::mapToCitation).toList();

        return SearchResultDto.builder().query(request.getQuery()).totalMatches(citations.size()).matches(citations).build();

    }

    private CitationDto mapToCitation(Document document) {

        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if (meta.get("documentId") != null) {
            try {
                docId = UUID.fromString(meta.get("documentId").toString());
            } catch (Exception ignore) {

            }
        }
        Integer chunkIndex = null;
        if (meta.get("chunkIndex") instanceof Number n) {
            chunkIndex = n.intValue();
        }

        Integer pageNumber = null;
        if (meta.get("pageNumber") instanceof Number n) {
            pageNumber = n.intValue();
        } else if (meta.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        }

        Double score = null;
        if (meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }
        return CitationDto.builder().documentId(docId).fileName((String) meta.getOrDefault("fileName", "Unknown")).chunkIndex(chunkIndex).pageNumber(pageNumber).snippet(document.getText()).similarityScore(score).metaData(meta).build();
    }




    private List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cannot be empty") String query, UUID documentId, Integer topK, Double similaritySearch) {

            int effectiveTopK = (topK != null && topK > 0) ? topK : appProperties.getRag().getTopK();

            double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appProperties.getRag().getSimilarityThreshold();

            SearchRequest.Builder searchRequestBuilder = SearchRequest.builder().
                    query(query).
                    topK(effectiveTopK);

            if (effectiveSimilarity > 0.0) {
                searchRequestBuilder.similarityThreshold(effectiveSimilarity);
            }


            //Filter.Expression filterExpression;
            if (documentId != null) {
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                //log.info("Filtering from document :");
                 searchRequestBuilder.filterExpression(

                                b.eq("documentId", documentId.toString())
                                        .build());
            }
            try {
                List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
                log.info("Retrieved {} chunks for query: '{}' (scoped docId: {})", documents.size(), query, documentId);
                return documents;
            } catch (Exception e) {
                log.error("Similarity search failed for query: '{}'", query, e);
                return Collections.emptyList();
            }


        }

    }


