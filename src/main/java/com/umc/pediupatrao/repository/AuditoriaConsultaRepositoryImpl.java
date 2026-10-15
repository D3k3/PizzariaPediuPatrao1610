package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

class AuditoriaConsultaRepositoryImpl implements AuditoriaConsultaRepository {
    private final MongoTemplate mongoTemplate;

    AuditoriaConsultaRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<AuditoriaEvento> consultar(FiltroAuditoria filtro, Pageable paginacao) {
        List<Criteria> criterios = new ArrayList<>();
        if (filtro.getEntidade() != null) {
            criterios.add(Criteria.where("entidade").is(filtro.getEntidade()));
        }
        if (temTexto(filtro.getEntidadeId())) {
            criterios.add(Criteria.where("entidadeId").is(filtro.getEntidadeId().trim()));
        }
        if (filtro.getOperacao() != null) {
            criterios.add(Criteria.where("operacao").is(filtro.getOperacao()));
        }
        if (temTexto(filtro.getUsuario())) {
            criterios.add(Criteria.where("usuario").is(filtro.getUsuario().trim()));
        }
        if (filtro.getDe() != null) {
            criterios.add(Criteria.where("dataHora").gte(filtro.getDe().atStartOfDay()));
        }
        if (filtro.getAte() != null) {
            criterios.add(Criteria.where("dataHora").lt(filtro.getAte().plusDays(1).atStartOfDay()));
        }

        Query query = criterios.isEmpty() ? new Query()
                : new Query(new Criteria().andOperator(criterios.toArray(Criteria[]::new)));
        long total = mongoTemplate.count(query, AuditoriaEvento.class);

        query.with(paginacao).with(Sort.by(Sort.Direction.DESC, "dataHora"));
        List<AuditoriaEvento> eventos = mongoTemplate.find(query, AuditoriaEvento.class);
        return PageableExecutionUtils.getPage(eventos, paginacao, () -> total);
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
