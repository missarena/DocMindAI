package com.example.docmind.service;

import com.example.docmind.dto.DocumentResponseDto;
import com.example.docmind.entity.DocumentMetadata;
import com.example.docmind.entity.DocumentStatus;
import com.example.docmind.repository.DocumentMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class DocumentMetadataService {
    private static final Logger log= LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocumentMetadataRepo documentMetadataRepo;
    private final DocumentParserService documentParserService;
    private final DocumentIngestionService documentIngestionService;
    private final JdbcTemplate jdbcTemplate;



    //method to upload and parse document
    public DocumentResponseDto uploadAndProcess(MultipartFile file){

        String fileName=file.getOriginalFilename()!=null?file.getOriginalFilename():"document";
        String contentType=file.getContentType()!=null?file.getContentType():"application/octat-stream";

        //document metadata create
        DocumentMetadata documentMetadata= DocumentMetadata
                .builder()
                .filename(fileName)
                .contentType(contentType)
                .status(DocumentStatus.UPLOADING)
                .fileSize(file.getSize())
                .build();

        //save the docuemnt metadata
     documentMetadata  =  documentMetadataRepo.save(documentMetadata);


     //parse the file
        List<Document> parsedDocs= documentParserService.parse(file);

        //ingest service
        int chunksCreated= documentIngestionService.ingest(documentMetadata,parsedDocs);

        return DocumentResponseDto.builder()
                .id(documentMetadata.getId())
                .fileName(documentMetadata.getFilename())
                .fileSize(documentMetadata.getFileSize())
                .chunksCreated(chunksCreated)
                .status(documentMetadata.getStatus())
                .message("Document successfully processed and indexed")
                .build();
    }
}
