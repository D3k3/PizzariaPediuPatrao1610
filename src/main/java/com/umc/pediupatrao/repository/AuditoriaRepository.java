package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.AuditoriaEvento;
import org.springframework.data.repository.Repository;

public interface AuditoriaRepository extends Repository<AuditoriaEvento, String>, AuditoriaConsultaRepository {
    AuditoriaEvento save(AuditoriaEvento evento);
}
