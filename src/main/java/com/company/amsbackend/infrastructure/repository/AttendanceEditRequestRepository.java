package com.company.amsbackend.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.company.amsbackend.domain.entity.AttendanceEditRequest;

import java.util.List;

public interface AttendanceEditRequestRepository extends MongoRepository <AttendanceEditRequest, String> {
    List<AttendanceEditRequest> findByEmployeeId(String employeeId);
}
