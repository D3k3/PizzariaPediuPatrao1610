package com.umc.pediupatrao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigracaoPerfisRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MigracaoPerfisRunner.class);
    private static final String COLECAO = "usuarios";
    private static final List<String> PERFIS_VALIDOS = List.of("ADMIN", "GERENTE", "ATENDENTE");

    private final MongoTemplate mongoTemplate;

    public MigracaoPerfisRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        long admins = mongoTemplate.updateMulti(
                new Query(Criteria.where("username").is("admin")
                        .and("role").nin(PERFIS_VALIDOS)),
                Update.update("role", "ADMIN"), COLECAO).getModifiedCount();

        long outros = mongoTemplate.updateMulti(
                new Query(Criteria.where("role").nin(PERFIS_VALIDOS)),
                Update.update("role", "ATENDENTE"), COLECAO).getModifiedCount();

        log.info("Migração de perfis: {} admin -> ADMIN, {} demais -> ATENDENTE", admins, outros);
    }
}
