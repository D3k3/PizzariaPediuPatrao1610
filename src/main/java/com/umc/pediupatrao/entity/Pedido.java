package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private TipoAtendimento tipoAtendimento;
    private List<ItemPedido> itens = new ArrayList<>();

    private BigDecimal valorBruto;
    private BigDecimal percentualDesconto;
    private BigDecimal valorTotal;

    private StatusPedido status;
    private List<HistoricoStatus> historicoStatus = new ArrayList<>();

    private String criadoPor;
    private LocalDateTime dataEntrada;
    private String responsavelSaida;
    private LocalDateTime dataSaida;

    private String justificativaCancelamento;
    private String canceladoPor;
    private LocalDateTime dataCancelamento;

    private String tipoPizza;
    private String sabor;
    private Integer quantidade;

    public boolean isCancelado() {
        return status == StatusPedido.CANCELADO;
    }

    public void registrarHistorico(StatusPedido novoStatus, LocalDateTime dataHora, String usuario) {
        historicoStatus.add(new HistoricoStatus(novoStatus, dataHora, usuario));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) { this.tipoAtendimento = tipoAtendimento; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens == null ? new ArrayList<>() : itens; }

    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }

    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public List<HistoricoStatus> getHistoricoStatus() { return historicoStatus; }
    public void setHistoricoStatus(List<HistoricoStatus> historicoStatus) {
        this.historicoStatus = historicoStatus == null ? new ArrayList<>() : historicoStatus;
    }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }

    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }

    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String canceladoPor) { this.canceladoPor = canceladoPor; }

    public LocalDateTime getDataCancelamento() { return dataCancelamento; }
    public void setDataCancelamento(LocalDateTime dataCancelamento) { this.dataCancelamento = dataCancelamento; }

    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
