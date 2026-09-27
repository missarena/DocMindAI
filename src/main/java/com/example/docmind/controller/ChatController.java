package com.example.docmind.controller;

import com.example.docmind.dto.*;
import com.example.docmind.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(
      name="Chat Management",
      description="All chat related apis goes here."
)
@RequiredArgsConstructor
public class ChatController {
     private final RagService ragService;


     @PostMapping
     public ResponseEntity<ApiResponse<ChatResponseDto>> askQuestion(
             @Valid @RequestBody ChatRequestDto requestDto
             ){
         ChatResponseDto chatResponseDto = ragService.askQuestion(requestDto);
         return ResponseEntity.ok(
                 ApiResponse.
                         <ChatResponseDto>
                         builder()
                         .success(true)
                         .message(null)
                         .data(chatResponseDto)
                         .timestamp(LocalDateTime.now())
                         .build()
         );
     }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream real-time Q&A answer tokens via Server-Sent Events (SSE)")
    public Flux<String> streamQuestion(
            @Valid @RequestBody ChatRequestDto requestDto

    ){
        //User user= (User) authentication.getPrincipal();
        return ragService.streamQuestionAnswer(requestDto);
    }

    @PostMapping("/search/similarity")
    @Operation(summary = "Perform semantic similarity search on stored document vectors")
    public ResponseEntity<ApiResponse<SearchResultDto>> searchSimilar(
            @Valid @RequestBody SearchRequestDto request


    ) {

        //User user= (User) authentication.getPrincipal();
        SearchResultDto results = ragService.searchSimilarChunks(request);
        return ResponseEntity.ok(
                ApiResponse.
                        <SearchResultDto>
                        builder()
                        .success(true)
                        .message(null)
                        .data(results)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
