package com.company.amsbackend.application.service;

import com.company.amsbackend.api.dto.AbsenceReportDto;
import com.company.amsbackend.api.dto.AgendaWithStatusDto;
import com.company.amsbackend.api.dto.AttendanceResponseDto;
import com.company.amsbackend.api.dto.DailyActivityDto;
import com.company.amsbackend.domain.entity.Attendance;
import com.microsoft.schemas.office.office.STInsetMode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    AttendanceResponseDto checkIn(String employeeId, List<String> agendaTitles, String location);
    AttendanceResponseDto checkOut(String employeeId, Map<String, Boolean> agendaCompletions, String remark, String referenceLink);

    List<AgendaWithStatusDto> getAgendasForAttendance(String attendanceId);
    List<Attendance> getAttendancesForEmployee(String employeeId, LocalDate from, LocalDate to);
    Map<String, Long> getDailyAttendanceSummary(LocalDate date);
    long getTotalWeeklyHours(String employeeId, LocalDate weekStart, LocalDate weekEnd);
    long getTotalMonthlyHours(String employeeId, int year, int month);
    List<DailyActivityDto> getDailyActivities(LocalDate date);

    void autoCheckoutForAll();

    AttendanceResponseDto getActiveSession(String employeeId);
    AbsenceReportDto getMonthlyAbsenceReport(String employeeId, int year, int month);
}