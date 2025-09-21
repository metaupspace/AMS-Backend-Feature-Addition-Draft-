package com.company.amsbackend.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AttendanceEditRequestDto {
    private String employeeId;
    private String attendanceId;

    private LocalDate date;

    private LocalDateTime requestCheckIn;

    private LocalDateTime requestCheckOut;

    private String reason;
}
