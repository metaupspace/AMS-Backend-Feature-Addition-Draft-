package com.company.amsbackend.application.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.company.amsbackend.api.dto.AttendanceEditRequestDto;
import com.company.amsbackend.application.service.AttendanceEditRequestService;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.AttendanceEditRequest;
import com.company.amsbackend.domain.enums.RequestStatus;
import com.company.amsbackend.infrastructure.repository.AttendanceEditRequestRepository;
import com.company.amsbackend.infrastructure.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceEditRequestServiceImpl implements AttendanceEditRequestService {

    @Autowired
    private final AttendanceEditRequestRepository attendanceEditRequestRepository;

    private final AttendanceRepository attendanceRepository;

    @Override
    public AttendanceEditRequestDto createRequest(AttendanceEditRequestDto requestDto) {

        if (requestDto.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Attendance edit requests cannot be created for future dates.");
        }
        if (requestDto.getRequestCheckIn() != null && requestDto.getRequestCheckOut() != null &&
                requestDto.getRequestCheckIn().isAfter(requestDto.getRequestCheckOut())) {
            throw new IllegalArgumentException("Check-in time must be before check-out time.");
        }

        List<AttendanceEditRequest> existingRequests = attendanceEditRequestRepository
                .findByEmployeeId(requestDto.getEmployeeId());

        boolean overlap = existingRequests.stream()
                .filter(req -> req.getDate().isEqual(requestDto.getDate()))
                .anyMatch(req -> timesOverlap(
                        requestDto.getRequestCheckIn(), requestDto.getRequestCheckOut(),
                        req.getRequestCheckIn(), req.getRequestCheckOut()));

        if (overlap) {
            throw new IllegalArgumentException("The requested check-in/out overlaps with an existing record.");
        }

        AttendanceEditRequest request = AttendanceEditRequest.builder()
                .employeeId(requestDto.getEmployeeId())
                .date(requestDto.getDate())
                .requestCheckIn(requestDto.getRequestCheckIn())
                .requestCheckOut(requestDto.getRequestCheckOut())
                .reason(requestDto.getReason())
                .status(RequestStatus.PENDING)
                .build();

        AttendanceEditRequest saved = attendanceEditRequestRepository.save(request);

        return mapToDto(saved);
    }

    @Override
    public AttendanceEditRequestDto reviewRequest(String requestId, String reviewedBy, boolean approved) {
        AttendanceEditRequest request = attendanceEditRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + requestId));

        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setStatus(approved ? RequestStatus.APPROVED : RequestStatus.REJECTED);

        if (approved) {
            LocalDate date = request.getDate();
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<Attendance> attendances = attendanceRepository
                    .findByEmployeeIdAndCheckInTimeBetween(request.getEmployeeId(), startOfDay, endOfDay);

            log.info("Found {} attendance records for employee {} on {}", attendances.size(), request.getEmployeeId(), date);
            if (attendances.isEmpty()) {
                throw new IllegalArgumentException("No attendance record found for employee on " + date);
            }

            Attendance attendance = attendances.get(0); // assuming 1 record per day
            attendance.setCheckInTime(request.getRequestCheckIn());
            attendance.setCheckOutTime(request.getRequestCheckOut());
            attendanceRepository.save(attendance);
        }

        AttendanceEditRequest updated = attendanceEditRequestRepository.save(request);
        return mapToDto(updated);
    }

    @Override
    public List<AttendanceEditRequest> getRequestsByEmployeeId(String employeeId) {
        List<AttendanceEditRequest> requests = attendanceEditRequestRepository.findByEmployeeId(employeeId);
        return requests;
    }

    @Override
    public List<AttendanceEditRequest> getAllRequests() {
        List<AttendanceEditRequest> requests = attendanceEditRequestRepository.findAll();
        return requests;
    }

    @Override
    public AttendanceEditRequest getRequestById(String requestId) {
        AttendanceEditRequest request = attendanceEditRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + requestId));
        return request;
    }

    private AttendanceEditRequestDto mapToDto(AttendanceEditRequest request) {
        return AttendanceEditRequestDto.builder()
                .employeeId(request.getEmployeeId())
                .date(request.getDate())
                .requestCheckIn(request.getRequestCheckIn())
                .requestCheckOut(request.getRequestCheckOut())
                .reason(request.getReason())
                .build();
    }

    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null)
            return false;
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

}
