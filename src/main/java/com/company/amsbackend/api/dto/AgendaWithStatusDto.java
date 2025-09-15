package com.company.amsbackend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaWithStatusDto {
    private String id;
    private String title;
    private boolean complete; // Changed from isComplete to match entity
}