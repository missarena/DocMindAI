package com.example.docmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDto {

    @NotBlank(message="Query cannot be empty")
    private String query;

    private UUID documentId;
    private Integer topK;
    private Double similaritySearch;



}
