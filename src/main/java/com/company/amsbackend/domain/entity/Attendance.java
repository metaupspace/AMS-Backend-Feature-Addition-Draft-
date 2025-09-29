package com.company.amsbackend.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import com.company.amsbackend.domain.enums.RequestStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendances")
@CompoundIndex(def = "{'employeeId': 1, 'checkInTime': -1}")
@CompoundIndex(def = "{'checkInTime': -1}")
public class Attendance {
    @Id
    private String id;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Check-in time is required")
    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private List<String> agendaIds; // Reference to Agenda IDs

    private String remark; // Optional remark on checkout
    private String referenceLink; // Mandatory on checkout

    private String checkInLocation;
    private boolean activeSession;

    private Long minutesWorked;
    
    private RequestStatus editRequestStatus;
    private String editRequestId;
}