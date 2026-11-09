package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    /** Pedidos com dataEntrada em [inicio, fim], ambos inclusivos. */
    @Query("{ 'dataEntrada': { $gte: ?0, $lte: ?1 } }")
    List<Pedido> findByDataEntradaBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Pedido> findTop100ByStatusOrderByDataEntradaDesc(StatusPedido status);
}
