package com.company.amsbackend.application.service;

import java.util.List;

import com.company.amsbackend.api.dto.AttendanceEditRequestDto;
import com.company.amsbackend.domain.entity.AttendanceEditRequest;

public interface AttendanceEditRequestService {
    AttendanceEditRequestDto createRequest(AttendanceEditRequestDto requestDto);
    AttendanceEditRequestDto reviewRequest(String requestId, String reviewedBy, boolean approved);
    List<AttendanceEditRequest> getRequestsByEmployeeId(String employeeId);
    List<AttendanceEditRequest> getAllRequests();
    AttendanceEditRequest getRequestById(String requestId);

}
