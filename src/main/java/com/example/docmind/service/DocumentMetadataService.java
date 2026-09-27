package com.example.docmind.service;

import com.example.docmind.Exception.DocumentProcessingException;
import com.example.docmind.Exception.ResourceNotFoundException;
import com.example.docmind.dto.ApiResponse;
import com.example.docmind.dto.DocumentMetaDataDto;
import com.example.docmind.dto.DocumentResponseDto;
import com.example.docmind.entity.DocumentMetadata;
import com.example.docmind.entity.DocumentStatus;
import com.example.docmind.repository.DocumentMetadataRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class DocumentMetadataService {
    private static final Logger log= LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocumentMetadataRepo documentMetadataRepo;
    private final DocumentParserService documentParserService;
    private final DocumentIngestionService documentIngestionService;
    private final JdbcTemplate jdbcTemplate;
    private final ModelMapper modelMapper;



    //method to upload and parse document
    @Transactional
    public DocumentResponseDto uploadAndProcess(MultipartFile file) {

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octat-stream";

        //document metadata create
        DocumentMetadata documentMetadata = DocumentMetadata
                .builder()
                .filename(fileName)
                .contentType(contentType)
                .status(DocumentStatus.UPLOADING)
                .fileSize(file.getSize())
                .build();

        //save the docuemnt metadata
        documentMetadata = documentMetadataRepo.save(documentMetadata);
        try {
            //parse the file
            List<Document> parsedDocs = documentParserService.parse(file);

            //ingest service
            int chunksCreated = documentIngestionService.ingest(documentMetadata, parsedDocs);

            return DocumentResponseDto.builder()
                    .id(documentMetadata.getId())
                    .fileName(documentMetadata.getFilename())
                    .fileSize(documentMetadata.getFileSize())
                    .chunksCreated(chunksCreated)
                    .status(documentMetadata.getStatus())
                    .message("Document successfully processed and indexed")
                    .build();
        } catch (DocumentProcessingException e) {
            log.error(
                    "Document processing failed for {}: {}",
                    fileName,
                    e.getMessage(),
                    e
            );

            throw e;
        }
    }

    public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files) {
        List<DocumentResponseDto> responseDtos = new ArrayList<>();

        for (MultipartFile file : files) {
            DocumentResponseDto result = this.uploadAndProcess(file);
            responseDtos.add(result);
        }

        return responseDtos;

    }

    public List<DocumentMetaDataDto> getAllDocuments() {

        List<DocumentMetadata> allDocuments = documentMetadataRepo.findAllByOrderByCreatedAtDesc();
        return allDocuments.stream()
                .map(documentMetadata -> modelMapper.map(documentMetadata, DocumentMetaDataDto.class))
                .toList();

    }


    public DocumentMetaDataDto getDocumentById(UUID id) {
        DocumentMetadata documentMetadata = documentMetadataRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));
        return modelMapper.map(documentMetadata, DocumentMetaDataDto.class);


    }


    @Transactional
    public void deleteDocument(UUID id) {

        // 1. Check whether document exists
        DocumentMetadata documentMetadata = documentMetadataRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found with id: " + id));

        // 2. Delete vector chunks belonging to this document
        try {
            String deleteVectorsSql =
                    "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";

            int deletedCount = jdbcTemplate.update(
                    deleteVectorsSql,
                    id.toString()
            );

            log.info(
                    "Deleted {} vector chunks for document id {}",
                    deletedCount,
                    id
            );

        } catch (Exception e) {
            log.warn(
                    "Could not delete vectors from vector_store directly: {}",
                    e.getMessage()
            );
        }

        // 3. Delete document metadata
        documentMetadataRepo.delete(documentMetadata);

        log.info("Deleted document metadata for id {}", id);
    }
}
