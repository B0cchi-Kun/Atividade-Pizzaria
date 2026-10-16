package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String status = StatusPedido.RECEBIDO.name();
    private String registradoPor;
    private Instant dataHoraEntrada;
    private String responsavelSaida;
    private Instant dataHoraSaida;
    private String canceladoPor;
    private Instant dataHoraCancelamento;
    private String justificativaCancelamento;

    // Campos legados, mantidos para leitura de pedidos já gravados na coleção.
    private String tipoPizza;
    private int quantidade;
    private String sabor;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public List<ItemPedido> getItens() { return itens == null ? List.of() : itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens == null ? new ArrayList<>() : itens; }
    public BigDecimal getSubtotal() { return subtotal == null ? BigDecimal.ZERO : subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getPercentualDesconto() { return percentualDesconto == null ? BigDecimal.ZERO : percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }
    public BigDecimal getValorDesconto() { return valorDesconto == null ? BigDecimal.ZERO : valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) { this.valorDesconto = valorDesconto; }
    public BigDecimal getValorTotal() { return valorTotal == null ? BigDecimal.ZERO : valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getStatus() { return status == null ? StatusPedido.RECEBIDO.name() : status; }
    public void setStatus(String status) { this.status = status; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public Instant getDataHoraEntrada() { return dataHoraEntrada; }
    public void setDataHoraEntrada(Instant dataHoraEntrada) { this.dataHoraEntrada = dataHoraEntrada; }
    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }
    public Instant getDataHoraSaida() { return dataHoraSaida; }
    public void setDataHoraSaida(Instant dataHoraSaida) { this.dataHoraSaida = dataHoraSaida; }
    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String canceladoPor) { this.canceladoPor = canceladoPor; }
    public Instant getDataHoraCancelamento() { return dataHoraCancelamento; }
    public void setDataHoraCancelamento(Instant dataHoraCancelamento) { this.dataHoraCancelamento = dataHoraCancelamento; }
    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }
    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }
}
