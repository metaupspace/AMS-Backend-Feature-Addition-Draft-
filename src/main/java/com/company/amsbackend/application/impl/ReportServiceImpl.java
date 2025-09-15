package com.company.amsbackend.application.impl;

import com.company.amsbackend.api.dto.AgendaWithStatusDto;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.application.service.ReportService;
import com.company.amsbackend.domain.entity.Agenda;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.exception.EmployeeNotFoundException;
import com.company.amsbackend.infrastructure.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final AgendaRepository agendaRepository;

    @Override
    public byte[] generateEmployeeMonthlyTimesheet(String employeeId, YearMonth yearMonth) {
        log.info("START generateEmployeeMonthlyTimesheet | employeeId: {} | yearMonth: {}", employeeId, yearMonth);

        try {
            Employee employee = employeeService.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

            LocalDate firstDayOfMonth = yearMonth.atDay(1);
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

            List<Attendance> attendances = attendanceService.getAttendancesForEmployee(
                    employeeId, firstDayOfMonth, lastDayOfMonth);

            log.debug("Attendance records found | employeeId: {} | yearMonth: {} | count: {}", employeeId, yearMonth, attendances.size());

            Map<LocalDate, List<Attendance>> attendancesByDate = attendances.stream()
                    .filter(a -> a.getCheckInTime() != null)
                    .collect(Collectors.groupingBy(a -> a.getCheckInTime().toLocalDate()));

            byte[] excelData = generateExcelReport(employee, yearMonth, attendancesByDate, firstDayOfMonth, lastDayOfMonth);

            log.info("END generateEmployeeMonthlyTimesheet | employeeId: {} | yearMonth: {}", employeeId, yearMonth);
            return excelData;

        } catch (Exception e) {
            log.error("Failed to generate timesheet | employeeId: {} | yearMonth: {} | error: {}", employeeId, yearMonth, e.getMessage(), e);
            throw new RuntimeException("Failed to generate timesheet", e);
        }
    }

    private byte[] generateExcelReport(Employee employee, YearMonth yearMonth,
                                       Map<LocalDate, List<Attendance>> attendancesByDate,
                                       LocalDate firstDayOfMonth, LocalDate lastDayOfMonth) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Timesheet");

            // Create styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle timeStyle = createTimeStyle(workbook);
            CellStyle wrapTextStyle = createWrapTextStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle dailyTotalStyle = createDailyTotalStyle(workbook);

            int rowNum = 0;

            // Add company title
            Row companyTitleRow = sheet.createRow(rowNum++);
            Cell companyTitleCell = companyTitleRow.createCell(0);
            companyTitleCell.setCellValue("METAUPSPACE LLP");
            companyTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9)); // Updated to span all columns including new ones

            // Add monthly timesheet title
            Row reportTitleRow = sheet.createRow(rowNum++);
            Cell reportTitleCell = reportTitleRow.createCell(0);
            reportTitleCell.setCellValue("MONTHLY TIMESHEET");
            reportTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9)); // Updated to span all columns including new ones

            // Empty row
            rowNum++;

            // Employee info header
            Row employeeRow = sheet.createRow(rowNum++);
            employeeRow.createCell(0).setCellValue("Employee Name:");
            employeeRow.createCell(1).setCellValue(employee.getName());
            employeeRow.getCell(0).setCellStyle(headerStyle);
            employeeRow.getCell(1).setCellStyle(normalStyle);

            Row monthRow = sheet.createRow(rowNum++);
            monthRow.createCell(0).setCellValue("Month:");
            monthRow.createCell(1).setCellValue(yearMonth.getMonth() + " " + yearMonth.getYear());
            monthRow.getCell(0).setCellStyle(headerStyle);
            monthRow.getCell(1).setCellStyle(normalStyle);

            // Empty row
            rowNum++;

            // Column headers
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Date", "Check In Time", "Check Out Time", "Total Time",
                    "Total Daily Hours", "Agendas", "Completed Agendas", "Reference Link", "Remark", "Validated Hours"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data tracking
            long totalMinutesWorked = 0;
            int totalDaysCheckedIn = 0;
            int totalLeaveDays = 0;

            // Iterate through all days of the month
            for (int day = 1; day <= lastDayOfMonth.getDayOfMonth(); day++) {
                LocalDate currentDate = yearMonth.atDay(day);
                List<Attendance> dayAttendances = attendancesByDate.getOrDefault(currentDate, Collections.emptyList());

                if (dayAttendances.isEmpty()) {
                    // No attendance for this day
                    totalLeaveDays++;
                    Row row = sheet.createRow(rowNum++);

                    // Date
                    Cell dateCell = row.createCell(0);
                    dateCell.setCellValue(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    dateCell.setCellStyle(dateStyle);

                    // Empty cells for check-in and check-out
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue("");
                    cell1.setCellStyle(normalStyle);

                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue("");
                    cell2.setCellStyle(normalStyle);

                    // Total time 00:00
                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue("00:00");
                    cell3.setCellStyle(normalStyle);

                    // Total Daily Hours - 00:00
                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue("00:00");
                    cell4.setCellStyle(dailyTotalStyle);

                    // Empty cells for remaining columns
                    for (int i = 5; i < headers.length; i++) {
                        Cell cell = row.createCell(i);
                        cell.setCellValue("");
                        cell.setCellStyle(normalStyle);
                    }
                } else {
                    // Sort attendances by check-in time for the day
                    dayAttendances.sort(Comparator.comparing(Attendance::getCheckInTime));

                    // Filter to only completed sessions
                    List<Attendance> completedAttendances = dayAttendances.stream()
                            .filter(a -> !a.isActiveSession() && a.getCheckInTime() != null && a.getCheckOutTime() != null)
                            .toList();

                    if (completedAttendances.isEmpty()) {
                        // Only active sessions, treat like a leave day
                        totalLeaveDays++;
                        Row row = sheet.createRow(rowNum++);

                        // Date
                        Cell dateCell = row.createCell(0);
                        dateCell.setCellValue(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        dateCell.setCellStyle(dateStyle);

                        // Empty cells for check-in and check-out
                        row.createCell(1).setCellValue("");
                        row.getCell(1).setCellStyle(normalStyle);
                        row.createCell(2).setCellValue("");
                        row.getCell(2).setCellStyle(normalStyle);

                        // Total time 00:00
                        row.createCell(3).setCellValue("00:00");
                        row.getCell(3).setCellStyle(normalStyle);

                        // Total Daily Hours - 00:00
                        row.createCell(4).setCellValue("00:00");
                        row.getCell(4).setCellStyle(dailyTotalStyle);

                        // Empty cells for remaining columns
                        for (int i = 5; i < headers.length; i++) {
                            Cell cell = row.createCell(i);
                            cell.setCellValue("");
                            cell.setCellStyle(normalStyle);
                        }
                    } else {
                        // Has completed attendance sessions
                        totalDaysCheckedIn++;

                        // Calculate total daily minutes worked
                        long dailyMinutesWorked = completedAttendances.stream()
                                .mapToLong(a -> a.getMinutesWorked() != null ?
                                        a.getMinutesWorked() :
                                        Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMinutes())
                                .sum();

                        totalMinutesWorked += dailyMinutesWorked;
                        String dailyHoursFormatted = String.format("%02d:%02d", dailyMinutesWorked / 60, dailyMinutesWorked % 60);

                        // Store first row index for this day to use for merging
                        int firstRowOfDay = rowNum;

                        for (int i = 0; i < completedAttendances.size(); i++) {
                            Attendance attendance = completedAttendances.get(i);
                            Row row = sheet.createRow(rowNum++);

                            // Date - only set value in first row for this day
                            Cell dateCell = row.createCell(0);
                            if (i == 0) {
                                // Only set the date value in the first row for this day
                                dateCell.setCellValue(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                            }
                            dateCell.setCellStyle(dateStyle);

                            // Check-in time
                            Cell checkInCell = row.createCell(1);
                            checkInCell.setCellValue(Date.from(attendance.getCheckInTime().atZone(ZoneId.systemDefault()).toInstant()));
                            checkInCell.setCellStyle(timeStyle);

                            // Check-out time
                            Cell checkOutCell = row.createCell(2);
                            checkOutCell.setCellValue(Date.from(attendance.getCheckOutTime().atZone(ZoneId.systemDefault()).toInstant()));
                            checkOutCell.setCellStyle(timeStyle);

                            // Calculate individual session duration
                            long minutes = attendance.getMinutesWorked() != null ?
                                    attendance.getMinutesWorked() :
                                    Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();

                            // Total time for individual session
                            String formattedDuration = String.format("%02d:%02d", minutes / 60, minutes % 60);
                            Cell totalTimeCell = row.createCell(3);
                            totalTimeCell.setCellValue(formattedDuration);
                            totalTimeCell.setCellStyle(normalStyle);

                            // Total Daily Hours - will be merged later
                            Cell dailyTotalCell = row.createCell(4);
                            if (i == 0) {
                                // Only set the value in the first row for this day
                                dailyTotalCell.setCellValue(dailyHoursFormatted);
                            }
                            dailyTotalCell.setCellStyle(dailyTotalStyle);

                            // Get agendas
                            List<AgendaWithStatusDto> agendas = formatAgendas(attendance.getAgendaIds());

                            // All agendas (with line breaks)
                            Cell agendasCell = row.createCell(5);
                            agendasCell.setCellValue(formatAgendasForExcel(agendas, false));
                            agendasCell.setCellStyle(wrapTextStyle);

                            // Completed agendas (with line breaks)
                            Cell completedAgendasCell = row.createCell(6);
                            completedAgendasCell.setCellValue(formatAgendasForExcel(agendas, true));
                            completedAgendasCell.setCellStyle(wrapTextStyle);

                            // Reference link
                            Cell refLinkCell = row.createCell(7);
                            refLinkCell.setCellValue(attendance.getReferenceLink() != null ? attendance.getReferenceLink() : "");
                            refLinkCell.setCellStyle(normalStyle);

                            // Remark
                            Cell remarkCell = row.createCell(8);
                            remarkCell.setCellValue(attendance.getRemark() != null ? attendance.getRemark() : "");
                            remarkCell.setCellStyle(wrapTextStyle);

                            // Validated Hours - empty column
                            Cell validatedHoursCell = row.createCell(9);
                            validatedHoursCell.setCellValue("");
                            validatedHoursCell.setCellStyle(normalStyle);
                        }

                        // Merge Date column and Daily Total Hours column if there are multiple entries for the day
                        if (completedAttendances.size() > 1) {
                            // Merge Date column (column 0)
                            sheet.addMergedRegion(new CellRangeAddress(
                                    firstRowOfDay, // first row
                                    rowNum - 1,    // last row
                                    0, 0           // date column
                            ));

                            // Merge Daily Total Hours column (column 4)
                            sheet.addMergedRegion(new CellRangeAddress(
                                    firstRowOfDay, // first row
                                    rowNum - 1,    // last row
                                    4, 4           // daily total column
                            ));
                        }
                    }
                }
            }

            // Add summary
            rowNum++; // Empty row

            Row summaryRow1 = sheet.createRow(rowNum++);
            summaryRow1.createCell(0).setCellValue("Total Days Present:");
            Cell presentCell = summaryRow1.createCell(1);
            presentCell.setCellValue(totalDaysCheckedIn);
            summaryRow1.getCell(0).setCellStyle(headerStyle);
            presentCell.setCellStyle(normalStyle);

            Row summaryRow2 = sheet.createRow(rowNum++);
            summaryRow2.createCell(0).setCellValue("Total Leave Days:");
            Cell leaveCell = summaryRow2.createCell(1);
            leaveCell.setCellValue(totalLeaveDays);
            summaryRow2.getCell(0).setCellStyle(headerStyle);
            leaveCell.setCellStyle(normalStyle);

            Row summaryRow3 = sheet.createRow(rowNum++);
            summaryRow3.createCell(0).setCellValue("Total Hours Worked:");
            String totalTimeFormatted = String.format("%02d:%02d", totalMinutesWorked / 60, totalMinutesWorked % 60);
            Cell hoursCell = summaryRow3.createCell(1);
            hoursCell.setCellValue(totalTimeFormatted);
            summaryRow3.getCell(0).setCellStyle(headerStyle);
            hoursCell.setCellStyle(normalStyle);

            // Add medium borders to the entire table
            addTableBorders(sheet, 0, rowNum - 1, 0, 9); // Updated to include new columns

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Set minimum width for agenda columns
                if (i == 5 || i == 6) {
                    sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 6000));
                }
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            log.info("Generated Excel timesheet for employee with {} days present, {} leave days, and {} hours worked",
                    totalDaysCheckedIn, totalLeaveDays, totalTimeFormatted);

            return outputStream.toByteArray();
        }
    }

    private void addTableBorders(Sheet sheet, int startRow, int endRow, int startCol, int endCol) {
        // This method adds medium borders around the entire table
        for (int rowNum = startRow; rowNum <= endRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            for (int colNum = startCol; colNum <= endCol; colNum++) {
                Cell cell = row.getCell(colNum);
                if (cell == null) continue;

                CellStyle style = cell.getCellStyle();
                CellStyle newStyle = sheet.getWorkbook().createCellStyle();
                newStyle.cloneStyleFrom(style);

                // Add medium border to cells on the edge of the table
                if (rowNum == startRow) {
                    newStyle.setBorderTop(BorderStyle.MEDIUM);
                }
                if (rowNum == endRow) {
                    newStyle.setBorderBottom(BorderStyle.MEDIUM);
                }
                if (colNum == startCol) {
                    newStyle.setBorderLeft(BorderStyle.MEDIUM);
                }
                if (colNum == endCol) {
                    newStyle.setBorderRight(BorderStyle.MEDIUM);
                }

                cell.setCellStyle(newStyle);
            }
        }
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDailyTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm AM/PM"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createWrapTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private List<AgendaWithStatusDto> formatAgendas(List<String> agendaIds) {
        if (agendaIds == null || agendaIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Agenda> agendaEntities = agendaRepository.findAllById(agendaIds);

        return agendaEntities.stream()
                .map(agenda -> AgendaWithStatusDto.builder()
                        .id(agenda.getId())
                        .title(agenda.getTitle())
                        .complete(agenda.isComplete())
                        .build())
                .collect(Collectors.toList());
    }

    private String formatAgendasForExcel(List<AgendaWithStatusDto> agendas, boolean onlyCompleted) {
        if (agendas.isEmpty()) {
            return "";
        }

        List<AgendaWithStatusDto> filteredAgendas = onlyCompleted ?
                agendas.stream().filter(AgendaWithStatusDto::isComplete).toList() :
                agendas;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filteredAgendas.size(); i++) {
            sb.append(i + 1).append(". ").append(filteredAgendas.get(i).getTitle());
            if (i < filteredAgendas.size() - 1) {
                sb.append("\n"); // Real line breaks work perfectly in Excel
            }
        }
        return sb.toString();
    }
}