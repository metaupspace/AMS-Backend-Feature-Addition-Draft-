package com.company.amsbackend.infrastructure.repository;

import com.company.amsbackend.domain.entity.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findByEmployeeIdAndCheckInTimeBetween(String employeeId, LocalDateTime from, LocalDateTime to);
    List<Attendance> findByEmployeeIdAndActiveSessionTrue(String employeeId);
    long countByEmployeeIdAndCheckInTimeBetween(String employeeId, LocalDateTime from, LocalDateTime to);
    List<Attendance> findByCheckInTimeBetween(LocalDateTime from, LocalDateTime to);
    List<Attendance> findByEmployeeId(String employeeId);

    // Optimized query to only fetch completed sessions for summary calculations
    @Query("{ 'checkInTime': { $gte: ?0, $lte: ?1 }, 'checkOutTime': { $ne: null }, 'activeSession': false }")
    List<Attendance> findCompletedSessionsBetween(LocalDateTime from, LocalDateTime to);

    // Query for bulk attendance summary data - only fetch required fields
    @Query(value = "{ 'checkInTime': { $gte: ?0, $lte: ?1 } }",
            fields = "{ 'employeeId': 1, 'checkInTime': 1, 'checkOutTime': 1, 'minutesWorked': 1, 'activeSession': 1 }")
    List<Attendance> findAttendanceSummaryBetween(LocalDateTime from, LocalDateTime to);
}