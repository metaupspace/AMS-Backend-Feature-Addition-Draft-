package com.company.amsbackend.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceReportDto {
    String EmployeeId;
    int year;
    int month;
    List<LocalDate> absenceDates;
}
