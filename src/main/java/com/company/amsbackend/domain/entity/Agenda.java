package com.company.amsbackend.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "agendas")
public class Agenda {
    @Id
    private String id;
    private String title;
    private boolean complete;

}