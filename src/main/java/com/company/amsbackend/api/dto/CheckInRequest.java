package com.company.amsbackend.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckInRequest {
    private String employeeId;
    private List<String> agendas; // List of agenda titles
    private String location;
}