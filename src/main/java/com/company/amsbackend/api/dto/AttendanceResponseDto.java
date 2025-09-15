package com.company.amsbackend.api.dto;

import com.company.amsbackend.domain.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDto {
    private Attendance attendance;
    private List<AgendaWithStatusDto> agendas;
}