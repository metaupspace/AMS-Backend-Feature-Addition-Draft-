package com.company.amsbackend.api.dto;

import com.company.amsbackend.domain.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryResponse {
    private String employeeId;
    private String name;
    private String email;
    private String contact;
    private EmployeeRole role;
    private boolean active;

    // Weekly/Monthly summary
    private long currentWeekMinutes;
    private long currentMonthMinutes;
    private int presentDaysThisMonth;
    private int totalWorkingDaysThisMonth;
    private List<LocalDate> presentDates;
}