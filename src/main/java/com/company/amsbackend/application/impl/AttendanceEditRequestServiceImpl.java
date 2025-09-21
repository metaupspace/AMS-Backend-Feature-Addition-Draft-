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
import com.company.amsbackend.domain.exception.AttendanceNotFoundException;
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

        LocalDate date = requestDto.getDate();
        List<Attendance> attendancesOfTheDay = attendanceRepository
                .findByEmployeeIdAndCheckInTimeBetween(requestDto.getEmployeeId(), date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay());

        boolean overlap = attendancesOfTheDay.stream()
                .filter(att -> att.getId() != null && !att.getId().equals(requestDto.getAttendanceId()))
                .anyMatch(att -> timesOverlap(
                        requestDto.getRequestCheckIn(), requestDto.getRequestCheckOut(),
                        att.getCheckInTime(), att.getCheckOutTime()));

        if (overlap) {
            throw new IllegalArgumentException("Requested time overlaps with another attendance record.");
        }

        AttendanceEditRequest editAttendanceRequest = AttendanceEditRequest.builder()
                .employeeId(requestDto.getEmployeeId())
                .attendanceId(requestDto.getAttendanceId())
                .date(requestDto.getDate())
                .requestCheckIn(requestDto.getRequestCheckIn())
                .requestCheckOut(requestDto.getRequestCheckOut())
                .reason(requestDto.getReason())
                .status(RequestStatus.PENDING)
                .build();

        AttendanceEditRequest savedEditAttendanceEditRequest = attendanceEditRequestRepository
                .save(editAttendanceRequest);

        return mapToDto(savedEditAttendanceEditRequest);
    }

    @Override
    public AttendanceEditRequestDto reviewRequest(String requestId, String reviewedBy, boolean approved) {
        AttendanceEditRequest request = attendanceEditRequestRepository.findById(requestId)
                .orElseThrow(() -> new AttendanceNotFoundException("Request not found with id: " + requestId));

        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setStatus(approved ? RequestStatus.APPROVED : RequestStatus.REJECTED);

        if (approved) {
            Attendance attendance = attendanceRepository.findById(request.getAttendanceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Attendance record not found with id: " + request.getAttendanceId()));

            attendance.setCheckInTime(request.getRequestCheckIn());
            attendance.setCheckOutTime(request.getRequestCheckOut());
            attendanceRepository.save(attendance);
        }

        AttendanceEditRequest UpdatedAttendance = attendanceEditRequestRepository.save(request);
        return mapToDto(UpdatedAttendance);
    }

    @Override
    public List<AttendanceEditRequest> getRequestsByEmployeeId(String employeeId) {
        return attendanceEditRequestRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<AttendanceEditRequest> getAllRequests() {
        return attendanceEditRequestRepository.findAll();

    }

    @Override
    public AttendanceEditRequest getRequestById(String requestId) {
        return attendanceEditRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + requestId));

    }

    private AttendanceEditRequestDto mapToDto(AttendanceEditRequest request) {
        return AttendanceEditRequestDto.builder()
                .employeeId(request.getEmployeeId())
                .attendanceId(request.getAttendanceId())
                .date(request.getDate())
                .requestCheckIn(request.getRequestCheckIn())
                .requestCheckOut(request.getRequestCheckOut())
                .reason(request.getReason())
                .build();
    }

    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1,
        LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

}
