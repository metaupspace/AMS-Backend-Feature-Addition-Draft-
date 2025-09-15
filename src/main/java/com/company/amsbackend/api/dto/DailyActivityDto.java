package com.company.amsbackend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivityDto {
    private String employeeId;
    private String employeeName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Long totalMinutesWorked;
    private List<AgendaWithStatusDto> agendas;
    private String remark;
    private String referenceLink;
    private boolean activeSession;
    private String checkInLocation;
}