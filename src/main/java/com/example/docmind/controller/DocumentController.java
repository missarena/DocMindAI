package com.example.docmind.controller;


import com.example.docmind.dto.ApiResponse;
import com.example.docmind.dto.DocumentMetaDataDto;
import com.example.docmind.dto.DocumentResponseDto;
import com.example.docmind.service.DocumentMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(
        name="Document Management",
        description="Endpoints for uploading,listing and managing documents and their vector embediings"
)
@RequiredArgsConstructor
public class DocumentController {


    private final DocumentMetadataService documentService;


    @PostMapping(value="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary="Upload and index a document (PDF,DOCX,TEXT,MD,CSV",
            description="This api is used to upload and index documents files"
    )
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocuments(
            @RequestParam("file")MultipartFile file
            ){

        //process the files
         DocumentResponseDto documentResponseDto=this.documentService.uploadAndProcess(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<DocumentResponseDto>builder()
                .success(true)
                .data(documentResponseDto)
                .timestamp(LocalDateTime.now())
                .message("Documents uploaded and indexed successfully")
                .build());

    }

    //    api to upload multiple documents
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and index multiple documents simultaneously",
            description = "This api is used to upload and index multiple documents."
    )
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
            @RequestParam("files")
            List<MultipartFile> files
    ) {

        //User user=(User)authentication.getPrincipal();
        List<DocumentResponseDto> responseDtos = documentService.uploadMultipleDocuments(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<List<DocumentResponseDto>>
                                        builder()
                                .message("Document uploaded successfully")
                                .success(true)
                                .timestamp(LocalDateTime.now())
                                .data(responseDtos)
                                .build()
                );
    }

    //    list all uploaded documents
//    Method for admin
    //@PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "List all uploaded documents and their indexing status")
    public ResponseEntity<ApiResponse<List<DocumentMetaDataDto>>> getAllDocuments(){
        List<DocumentMetaDataDto> documents =documentService.getAllDocuments();
        return ResponseEntity.ok(
                ApiResponse.<List<DocumentMetaDataDto>>
                                builder()
                        .message("All documents is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(documents)
                        .build()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get metadata of a specific document by ID")
    public ResponseEntity<ApiResponse<DocumentMetaDataDto>> getDocumentById(@PathVariable UUID id) {
        DocumentMetaDataDto document = documentService.getDocumentById(id);
        return ResponseEntity.ok(
                ApiResponse.<DocumentMetaDataDto>
                                builder()
                        .message("Single document is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(document)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and purge its vector embeddings from vector store")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>
                                builder()
                        .message("Document deleted successfully")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build()
        );
    }




}
