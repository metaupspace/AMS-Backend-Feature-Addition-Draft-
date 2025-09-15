package com.company.amsbackend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaCompletionDto {
    private String agendaId;
    private boolean complete; // Changed from isComplete
}