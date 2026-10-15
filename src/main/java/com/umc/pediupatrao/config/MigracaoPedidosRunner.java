package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.StatusPedido;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MigracaoPedidosRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MigracaoPedidosRunner.class);
    private static final String COLECAO = "pedidos";

    private static final Map<String, StatusPedido> EQUIVALENTES = Map.of(
            "EM_PREPARO", StatusPedido.EM_PREPARACAO,
            "ENTREGUE", StatusPedido.FINALIZADO);

    private final MongoTemplate mongoTemplate;

    public MigracaoPedidosRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        EQUIVALENTES.forEach((antigo, novo) -> {
            long n = mongoTemplate.updateMulti(new Query(Criteria.where("status").is(antigo)),
                    Update.update("status", novo.name()), COLECAO).getModifiedCount();
            if (n > 0) {
                log.info("Migração de pedidos: {} com status {} -> {}", n, antigo, novo);
            }
        });

        List<String> validos = Arrays.stream(StatusPedido.values()).map(Enum::name).toList();
        long desconhecidos = mongoTemplate.updateMulti(new Query(Criteria.where("status").nin(validos)),
                Update.update("status", StatusPedido.RECEBIDO.name()), COLECAO).getModifiedCount();
        if (desconhecidos > 0) {
            log.info("Migração de pedidos: {} com status nulo ou desconhecido -> RECEBIDO", desconhecidos);
        }
    }
}
