package com.company.amsbackend.api.dto;

import lombok.Data;

@Data
public class MonthlyReportRequest {
    private int year;
    private int month;
}