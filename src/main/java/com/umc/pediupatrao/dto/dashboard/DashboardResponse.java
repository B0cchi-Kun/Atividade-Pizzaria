package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardResponse {
    private DashboardResponse() {}

    public record StatusPedido(String status, String descricao, long quantidade) {}
    public record Resumo(
            LocalDate inicio,
            LocalDate fim,
            long totalPedidos,
            long pedidosNaoCancelados,
            long pedidosConcluidos,
            long pedidosCancelados,
            BigDecimal faturamentoLiquido,
            BigDecimal ticketMedio,
            List<StatusPedido> pedidosPorStatus) {}
    public record VendaPeriodo(String periodo, long pedidos, BigDecimal faturamento) {}
    public record Vendas(LocalDate inicio, LocalDate fim, String agrupamento, List<VendaPeriodo> serie) {}
    public record ProdutoRanking(String produtoId, String produto, long quantidadeVendida, BigDecimal receitaGerada) {}
    public record ProdutosMaisVendidos(LocalDate inicio, LocalDate fim, List<ProdutoRanking> produtos) {}
}
