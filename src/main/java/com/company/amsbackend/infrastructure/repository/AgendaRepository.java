package com.company.amsbackend.infrastructure.repository;

import com.company.amsbackend.domain.entity.Agenda;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AgendaRepository extends MongoRepository<Agenda, String> {
}