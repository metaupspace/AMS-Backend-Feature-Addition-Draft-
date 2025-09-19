package com.company.amsbackend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AttendanceEditReviewDto {
     
    private String reviewedBy;

    private boolean approved; 
}
