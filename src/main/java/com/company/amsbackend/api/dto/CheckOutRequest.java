package com.company.amsbackend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {
    private String employeeId;
    private List<AgendaCompletionDto> agendaCompletions;
    private String remark;
    private String referenceLink;
}