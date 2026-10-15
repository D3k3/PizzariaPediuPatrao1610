package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditoriaConsultaRepository {
    Page<AuditoriaEvento> consultar(FiltroAuditoria filtro, Pageable paginacao);
}
