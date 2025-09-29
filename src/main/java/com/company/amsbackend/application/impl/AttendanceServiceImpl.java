package com.company.amsbackend.application.impl;

import com.company.amsbackend.api.dto.AbsenceReportDto;
import com.company.amsbackend.api.dto.AgendaWithStatusDto;
import com.company.amsbackend.api.dto.AttendanceResponseDto;
import com.company.amsbackend.api.dto.DailyActivityDto;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.domain.entity.Agenda;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.exception.AttendanceNotFoundException;
import com.company.amsbackend.domain.exception.DomainException;
import com.company.amsbackend.domain.exception.EmployeeNotFoundException;
import com.company.amsbackend.infrastructure.repository.AgendaRepository;
import com.company.amsbackend.infrastructure.repository.AttendanceRepository;
import com.company.amsbackend.infrastructure.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AgendaRepository agendaRepository;

    private static final int DAILY_CHECK_IN_LIMIT = 10;

    @Override
    @Transactional
    public AttendanceResponseDto checkIn(String employeeId, List<String> agendaTitles, String location) {
        log.info("START checkIn | employeeId: {} | location: {} | agendas: {}", employeeId, location, agendaTitles);

        // Verify employee exists
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            log.error("Employee not found | employeeId: {}", employeeId);
            throw new EmployeeNotFoundException(employeeId);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

        long count = attendanceRepository.countByEmployeeIdAndCheckInTimeBetween(employeeId, startOfDay, endOfDay);
        if (count >= DAILY_CHECK_IN_LIMIT) {
            log.warn("Daily check-in limit reached | employeeId: {}", employeeId);
            throw new DomainException("Check-in limit reached for today");
        }

        List<Attendance> activeSessions = attendanceRepository.findByEmployeeIdAndActiveSessionTrue(employeeId);
        if (!activeSessions.isEmpty()) {
            log.warn("Active session exists | employeeId: {}", employeeId);
            throw new DomainException("Already checked in! Please check-out first.");
        }

        if (agendaTitles == null || agendaTitles.isEmpty()) {
            log.warn("Agenda is required for check-in | employeeId: {}", employeeId);
            throw new DomainException("At least one agenda is required for check-in");
        }

        // Create and save agendas with initial completion status as false
        List<Agenda> agendas = agendaTitles.stream()
                .map(title -> Agenda.builder()
                        .title(title)
                        .complete(false)
                        .build())
                .collect(Collectors.toList());

        List<Agenda> savedAgendas = agendaRepository.saveAll(agendas);
        log.info("Agendas created | count: {} | employeeId: {}", savedAgendas.size(), employeeId);

        List<String> agendaIds = savedAgendas.stream()
                .map(Agenda::getId)
                .collect(Collectors.toList());

        // Create and save attendance record
        Attendance attendance = Attendance.builder()
                .employeeId(employeeId)
                .checkInTime(LocalDateTime.now())
                .agendaIds(agendaIds)
                .checkInLocation(location)
                .activeSession(true)
                .build();

        attendance = attendanceRepository.save(attendance);
        log.info("Check-in successful | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());

        // Create response with agendas
        List<AgendaWithStatusDto> agendaWithStatusDtos = savedAgendas.stream()
                .map(agenda -> AgendaWithStatusDto.builder()
                        .id(agenda.getId())
                        .title(agenda.getTitle())
                        .complete(agenda.isComplete()) // Uses the getter method
                        .build())
                .collect(Collectors.toList());

        log.info("END checkIn | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());
        return AttendanceResponseDto.builder()
                .attendance(attendance)
                .agendas(agendaWithStatusDtos)
                .build();
    }

    @Override
    public List<AgendaWithStatusDto> getAgendasForAttendance(String attendanceId) {
        log.info("START getAgendasForAttendance | attendanceId: {}", attendanceId);

        Optional<Attendance> attendanceOpt = attendanceRepository.findById(attendanceId);
        if (attendanceOpt.isEmpty()) {
            log.error("Attendance not found | attendanceId: {}", attendanceId);
            throw new AttendanceNotFoundException("Attendance not found with ID: " + attendanceId);
        }

        Attendance attendance = attendanceOpt.get();
        List<String> agendaIds = attendance.getAgendaIds();

        if (agendaIds == null || agendaIds.isEmpty()) {
            log.info("No agendas for attendance | attendanceId: {}", attendanceId);
            return new ArrayList<>();
        }

        List<Agenda> agendas = agendaRepository.findAllById(agendaIds);
        log.info("Agendas found | count: {} | attendanceId: {}", agendas.size(), attendanceId);

        return agendas.stream()
                .map(agenda -> AgendaWithStatusDto.builder()
                        .id(agenda.getId())
                        .title(agenda.getTitle())
                        .complete(agenda.isComplete())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttendanceResponseDto checkOut(
            String employeeId,
            Map<String, Boolean> agendaCompletions,
            String remark,
            String referenceLink
    ) {
        log.info("START checkOut | employeeId: {} | remark: {} | referenceLink: {}", employeeId, remark, referenceLink);

        // Verify employee exists
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            log.error("Employee not found | employeeId: {}", employeeId);
            throw new EmployeeNotFoundException(employeeId);
        }

        // Find active session
        List<Attendance> activeSessions = attendanceRepository.findByEmployeeIdAndActiveSessionTrue(employeeId);
        if (activeSessions.isEmpty()) {
            log.warn("No active session found for check-out | employeeId: {}", employeeId);
            throw new DomainException("No active session found. Please check-in first.");
        }

        Attendance attendance = activeSessions.get(0);

        if (referenceLink == null || referenceLink.trim().isEmpty()) {
            log.warn("Reference link required for check-out | employeeId: {}", employeeId);
            throw new DomainException("Reference link is required for check-out");
        }

        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setRemark(remark);
        attendance.setReferenceLink(referenceLink);
        attendance.setActiveSession(false);

        // Calculate and set minutesWorked
        if (attendance.getCheckInTime() != null) {
            long minutes = Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
            attendance.setMinutesWorked(minutes);
            log.debug("Worked minutes calculated | attendanceId: {} | minutes: {}", attendance.getId(), minutes);
        }

        List<String> agendaIds = attendance.getAgendaIds();
        if (agendaIds != null && !agendaIds.isEmpty()) {
            // Verify that all agendas from the attendance record are included in the completion map
            if (agendaCompletions == null || agendaCompletions.isEmpty()) {
                log.error("Agenda completion status missing | employeeId: {}", employeeId);
                throw new DomainException("Agenda completion status is required for all agendas");
            }

            // Check if any agenda IDs are missing from the completion status map
            List<String> missingAgendaIds = agendaIds.stream()
                    .filter(id -> !agendaCompletions.containsKey(id))
                    .collect(Collectors.toList());

            if (!missingAgendaIds.isEmpty()) {
                log.error("Missing completion status for agendas | employeeId: {} | missing: {}", employeeId, missingAgendaIds);
                throw new DomainException("Missing completion status for " + missingAgendaIds.size() + " agenda(s).");
            }

            // Fetch all agendas that need to be updated
            List<Agenda> agendas = agendaRepository.findAllById(agendaIds);

            if (agendas.size() != agendaIds.size()) {
                log.error("Some agendas not found | attendanceId: {}", attendance.getId());
                throw new DomainException("Some agendas could not be found. Please contact support.");
            }

            // Update each agenda with its completion status
            for (Agenda agenda : agendas) {
                Boolean isComplete = agendaCompletions.get(agenda.getId());
                log.debug("Set agenda complete status | agendaId: {} | was: {} | now: {}", agenda.getId(), agenda.isComplete(), isComplete);
                agenda.setComplete(isComplete != null && isComplete);
            }

            // Save all updated agendas and verify the save was successful
            List<Agenda> savedAgendas = agendaRepository.saveAll(agendas);
            log.info("Agendas updated | count: {} | employeeId: {}", savedAgendas.size(), employeeId);

            // Verify if the updates were successful by re-fetching
            List<Agenda> verifiedAgendas = agendaRepository.findAllById(agendaIds);
            for (Agenda agenda : verifiedAgendas) {
                log.debug("Verified agenda ID: {} title: '{}' has completion status: {}",
                        agenda.getId(), agenda.getTitle(), agenda.isComplete());
            }

            // Create response DTOs - fixed to use correct property name
            List<AgendaWithStatusDto> agendaWithStatusDtos = verifiedAgendas.stream()
                    .map(agenda -> AgendaWithStatusDto.builder()
                            .id(agenda.getId())
                            .title(agenda.getTitle())
                            .complete(agenda.isComplete())
                            .build())
                    .collect(Collectors.toList());

            attendance = attendanceRepository.save(attendance);
            log.info("Check-out successful | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());

            log.info("END checkOut | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());
            return AttendanceResponseDto.builder()
                    .attendance(attendance)
                    .agendas(agendaWithStatusDtos)
                    .build();
        } else {
            attendance = attendanceRepository.save(attendance);
            log.info("Check-out successful (no agendas) | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());

            log.info("END checkOut | employeeId: {} | attendanceId: {}", employeeId, attendance.getId());
            return AttendanceResponseDto.builder()
                    .attendance(attendance)
                    .agendas(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public Map<String, Long> getDailyAttendanceSummary(LocalDate date) {
        log.info("START getDailyAttendanceSummary | date: {}", date);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusSeconds(1);
        List<Attendance> all = attendanceRepository.findByCheckInTimeBetween(start, end);
        log.debug("Attendance records found | date: {} | count: {}", date, all.size());

        Map<String, Long> summary = all.stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId, Collectors.counting()));

        log.info("END getDailyAttendanceSummary | date: {} | employeeCount: {}", date, summary.size());
        return summary;
    }

    @Override
    public long getTotalWeeklyHours(String employeeId, LocalDate weekStart, LocalDate weekEnd) {
        log.debug("START getTotalWeeklyHours | employeeId: {} | from: {} | to: {}", employeeId, weekStart, weekEnd);

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndCheckInTimeBetween(
                employeeId,
                weekStart.atStartOfDay(),
                weekEnd.plusDays(1).atStartOfDay().minusSeconds(1)
        );

        long totalMinutes = attendances.stream()
                .filter(a -> a.getCheckInTime() != null && a.getCheckOutTime() != null && !a.isActiveSession())
                .mapToLong(a -> a.getMinutesWorked() != null ? a.getMinutesWorked() :
                        Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMinutes())
                .sum();

        log.info("END getTotalWeeklyHours | employeeId: {} | totalMinutes: {}", employeeId, totalMinutes);
        return totalMinutes;
    }

    @Override
    public long getTotalMonthlyHours(String employeeId, int year, int month) {
        log.debug("START getTotalMonthlyHours | employeeId: {} | year: {} | month: {}", employeeId, year, month);

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        long totalMinutes = getTotalWeeklyHours(employeeId, firstDay, lastDay);

        log.info("END getTotalMonthlyHours | employeeId: {} | year: {} | month: {} | totalMinutes: {}", employeeId, year, month, totalMinutes);
        return totalMinutes;
    }

    @Override
    public List<Attendance> getAttendancesForEmployee(String employeeId, LocalDate from, LocalDate to) {
        log.debug("START getAttendancesForEmployee | employeeId: {} | from: {} | to: {}", employeeId, from, to);

        // Verify employee exists - only if needed for validation
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            log.error("Employee not found | employeeId: {}", employeeId);
            throw new EmployeeNotFoundException(employeeId);
        }

        List<Attendance> records;
        if (from == null && to == null) {
            records = attendanceRepository.findByEmployeeId(employeeId);
        } else if (from == null) {
            LocalDateTime toDateTime = to.plusDays(1).atStartOfDay().minusSeconds(1);
            records = attendanceRepository.findByEmployeeIdAndCheckInTimeBetween(
                    employeeId, LocalDate.MIN.atStartOfDay(), toDateTime
            );
        } else if (to == null) {
            LocalDateTime fromDateTime = from.atStartOfDay();
            records = attendanceRepository.findByEmployeeIdAndCheckInTimeBetween(
                    employeeId, fromDateTime, LocalDate.MAX.atStartOfDay().minusSeconds(1)
            );
        } else {
            records = attendanceRepository.findByEmployeeIdAndCheckInTimeBetween(
                    employeeId,
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay().minusSeconds(1)
            );
        }
        log.info("Attendance records found | employeeId: {} | count: {}", employeeId, records.size());
        return records;
    }

    @Override
    public List<DailyActivityDto> getDailyActivities(LocalDate date) {
        log.info("START getDailyActivities | date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Attendance> attendances = attendanceRepository.findByCheckInTimeBetween(startOfDay, endOfDay);
        log.debug("Attendance records found | date: {} | count: {}", date, attendances.size());

        if (attendances.isEmpty()) {
            log.info("No activities found | date: {}", date);
            return Collections.emptyList();
        }

        Set<String> employeeIds = attendances.stream()
                .map(Attendance::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Employee> employeeMap = employeeRepository.findAll().stream()
                .filter(emp -> employeeIds.contains(emp.getEmployeeId()))
                .collect(Collectors.toMap(Employee::getEmployeeId, emp -> emp));

        Set<String> allAgendaIds = attendances.stream()
                .filter(a -> a.getAgendaIds() != null)
                .flatMap(a -> a.getAgendaIds().stream())
                .collect(Collectors.toSet());

        Map<String, Agenda> agendaMap = Collections.emptyMap();
        if (!allAgendaIds.isEmpty()) {
            agendaMap = agendaRepository.findAllById(allAgendaIds).stream()
                    .collect(Collectors.toMap(Agenda::getId, agenda -> agenda));
        }

        List<DailyActivityDto> activities = new ArrayList<>();
        for (Attendance attendance : attendances) {
            Employee employee = employeeMap.get(attendance.getEmployeeId());
            if (employee == null) {
                log.warn("Employee not found for attendance | attendanceId: {} | employeeId: {}", attendance.getId(), attendance.getEmployeeId());
                continue;
            }
            List<AgendaWithStatusDto> agendas = new ArrayList<>();
            if (attendance.getAgendaIds() != null && !attendance.getAgendaIds().isEmpty()) {
                agendas = attendance.getAgendaIds().stream()
                        .map(agendaMap::get)
                        .filter(Objects::nonNull)
                        .map(agenda -> AgendaWithStatusDto.builder()
                                .id(agenda.getId())
                                .title(agenda.getTitle())
                                .complete(agenda.isComplete())
                                .build())
                        .collect(Collectors.toList());
            }

            DailyActivityDto activityDto = DailyActivityDto.builder()
                    .employeeId(employee.getEmployeeId())
                    .employeeName(employee.getName())
                    .checkInTime(attendance.getCheckInTime())
                    .checkOutTime(attendance.getCheckOutTime())
                    .totalMinutesWorked(attendance.getMinutesWorked())
                    .agendas(agendas)
                    .remark(attendance.getRemark())
                    .referenceLink(attendance.getReferenceLink())
                    .activeSession(attendance.isActiveSession())
                    .checkInLocation(attendance.getCheckInLocation())
                    .build();

            activities.add(activityDto);
        }
        activities.sort(Comparator.comparing(DailyActivityDto::getCheckInTime));
        log.info("END getDailyActivities | date: {} | activityCount: {}", date, activities.size());
        return activities;
    }

    @Override
    @Scheduled(cron = "0 59 11 * * *" , zone = "Asia/Kolkata")
    public void autoCheckoutForAll() {
        log.info("START autoCheckoutForAll | time: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        List<Attendance> activeSessions = attendanceRepository.findByCheckInTimeBetween(
                now.minusDays(7), now
        ).stream().filter(Attendance::isActiveSession).toList();

        log.info("Active sessions to auto-checkout | count: {}", activeSessions.size());

        for (Attendance attendance : activeSessions) {
            try {
                log.debug("Auto-checkout for attendance | attendanceId: {} | employeeId: {}", attendance.getId(), attendance.getEmployeeId());
                attendance.setCheckOutTime(now);
                attendance.setActiveSession(false);
                attendance.setRemark("Auto checkout - Daily cutoff");

                if (attendance.getCheckInTime() != null) {
                    long minutes = Duration.between(attendance.getCheckInTime(), now).toMinutes();
                    attendance.setMinutesWorked(minutes);
                }

                attendanceRepository.save(attendance);
                log.info("Auto-checkout complete | attendanceId: {} | employeeId: {}", attendance.getId(), attendance.getEmployeeId());

                List<String> newAgendaIds = new ArrayList<>();
                if (attendance.getAgendaIds() != null && !attendance.getAgendaIds().isEmpty()) {
                    List<Agenda> oldAgendas = agendaRepository.findAllById(attendance.getAgendaIds());
                    List<Agenda> newAgendas = new ArrayList<>();
                    for (Agenda oldAgenda : oldAgendas) {
                        if (!oldAgenda.isComplete()) {
                            Agenda newAgenda = Agenda.builder()
                                    .title(oldAgenda.getTitle())
                                    .complete(false)
                                    .build();
                            newAgendas.add(newAgenda);
                        }
                    }
                    if (!newAgendas.isEmpty()) {
                        List<Agenda> savedAgendas = agendaRepository.saveAll(newAgendas);
                        newAgendaIds = savedAgendas.stream()
                                .map(Agenda::getId)
                                .collect(Collectors.toList());
                        log.debug("Incomplete agendas copied | count: {} | employeeId: {}", newAgendaIds.size(), attendance.getEmployeeId());
                    }
                }

                Attendance newSession = Attendance.builder()
                        .employeeId(attendance.getEmployeeId())
                        .checkInTime(now.plusMinutes(2))
                        .agendaIds(newAgendaIds)
                        .checkInLocation(attendance.getCheckInLocation())
                        .activeSession(true)
                        .build();

                Attendance savedNewSession = attendanceRepository.save(newSession);
                log.info("Auto-check-in created | employeeId: {} | attendanceId: {} | agendaCount: {}",
                        attendance.getEmployeeId(), savedNewSession.getId(), newAgendaIds.size());
            } catch (Exception e) {
                log.error("Error during auto-checkout | attendanceId: {} | error: {}", attendance.getId(), e.getMessage(), e);
            }
        }
        log.info("END autoCheckoutForAll");
    }

    @Override
    public AttendanceResponseDto getActiveSession(String employeeId) {
        log.info("START getActiveSession | employeeId: {}", employeeId);

        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            log.error("Employee not found | employeeId: {}", employeeId);
            throw new EmployeeNotFoundException(employeeId);
        }

        List<Attendance> activeSessions = attendanceRepository.findByEmployeeIdAndActiveSessionTrue(employeeId);

        if (activeSessions.isEmpty()) {
            log.info("No active session found | employeeId: {}", employeeId);
            throw new DomainException("No active session found for employee ID: " + employeeId);
        }

        Attendance activeAttendance = activeSessions.get(0);

        List<AgendaWithStatusDto> agendaWithStatusDtos = new ArrayList<>();
        if (activeAttendance.getAgendaIds() != null && !activeAttendance.getAgendaIds().isEmpty()) {
            List<Agenda> agendas = agendaRepository.findAllById(activeAttendance.getAgendaIds());

            agendaWithStatusDtos = agendas.stream()
                    .map(agenda -> AgendaWithStatusDto.builder()
                            .id(agenda.getId())
                            .title(agenda.getTitle())
                            .complete(agenda.isComplete())
                            .build())
                    .collect(Collectors.toList());
            log.debug("Agendas for active session | count: {}", agendaWithStatusDtos.size());
        }

        log.info("END getActiveSession | employeeId: {} | attendanceId: {}", employeeId, activeAttendance.getId());
        return AttendanceResponseDto.builder()
                .attendance(activeAttendance)
                .agendas(agendaWithStatusDtos)
                .build();
    }


    
    @Override
    @Transactional(readOnly = true)
    public AbsenceReportDto getMonthlyAbsenceReport(String employeeId, int year, int month) {
        employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found | employeeId: {}", employeeId);
                    return new EmployeeNotFoundException(employeeId);
                });
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndCheckInTimeBetween(
                employeeId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        Set<LocalDate> presentDates = attendances.stream()
                .map(att -> att.getCheckInTime().toLocalDate())
                .collect(Collectors.toSet());

        List<LocalDate> absentDates = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !presentDates.contains(date))
                .toList();

        AbsenceReportDto response = new AbsenceReportDto(
                employeeId,
                year,
                month,
                absentDates
        );
        return response;
    }


    @Override
    public Optional<Attendance> findById(String attendanceId){
        return attendanceRepository.findById(attendanceId);
    }

    @Override
    public Attendance save(Attendance attendance){
        return attendanceRepository.save(attendance);
    }
}