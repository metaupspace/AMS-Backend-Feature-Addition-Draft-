package com.company.amsbackend.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.company.amsbackend.domain.enums.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance_edit_requests")
@Builder
public class AttendanceEditRequest {
    @Id
    private String id;
    private String employeeId;

    private String attendanceId;
    
    private  LocalDate date;

    private LocalDateTime requestCheckIn;
    private LocalDateTime requestCheckOut;

    private String reviewedBy; // HR or Manager who reviewed the request
    private LocalDateTime reviewedAt;

    private String reason;
    private RequestStatus status;
    

}
