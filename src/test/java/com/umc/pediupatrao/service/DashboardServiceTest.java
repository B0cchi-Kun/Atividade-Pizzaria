package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Resumo;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Vendas;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private List<Pedido> pedidos;
    private int consultas;
    private Instant inicioConsultado;
    private Instant fimConsultado;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZONE);
        PedidoRepository repositorioFalso = (PedidoRepository) Proxy.newProxyInstance(
                PedidoRepository.class.getClassLoader(),
                new Class<?>[] {PedidoRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals(
                            "findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThanOrderByDataHoraEntradaAsc")) {
                        consultas++;
                        inicioConsultado = (Instant) args[0];
                        fimConsultado = (Instant) args[1];
                        return pedidos;
                    }
                    if (method.getName().equals("findByClienteId")) {
                        return List.of();
                    }
                    return null;
                });
        dashboardService = new DashboardService(repositorioFalso, clock);
    }

    @Test
    void resumoCalculaVendasTicketMedioEEstadosSemSomarCancelamentos() {
        pedidos = List.of(
                pedido("RECEBIDO", "100.00", "2026-11-02T15:00:00Z"),
                pedido("CANCELADO", "50.00", "2026-11-03T15:00:00Z"),
                pedido("FINALIZADO", "60.00", "2026-11-04T15:00:00Z"));

        Resumo resumo = dashboardService.resumo(null, null);

        assertEquals(LocalDate.parse("2026-10-11"), resumo.inicio());
        assertEquals(LocalDate.parse("2026-11-09"), resumo.fim());
        assertEquals(3, resumo.totalPedidos());
        assertEquals(2, resumo.pedidosNaoCancelados());
        assertEquals(1, resumo.pedidosConcluidos());
        assertEquals(1, resumo.pedidosCancelados());
        assertEquals(0, new BigDecimal("160.00").compareTo(resumo.faturamentoLiquido()));
        assertEquals(0, new BigDecimal("80.00").compareTo(resumo.ticketMedio()));
        assertEquals(1, resumo.pedidosPorStatus().stream()
                .filter(status -> status.status().equals("CANCELADO")).findFirst().orElseThrow().quantidade());
        assertEquals(1, consultas);
        assertEquals(Instant.parse("2026-10-11T03:00:00Z"), inicioConsultado);
        assertEquals(Instant.parse("2026-11-10T03:00:00Z"), fimConsultado);
    }

    @Test
    void serieDiariaIncluiDiasSemVendaEIgnoraCancelados() {
        pedidos = List.of(
                pedido("PRONTO", "45.50", "2026-11-02T16:00:00Z"),
                pedido("CANCELADO", "100.00", "2026-11-03T17:00:00Z"));

        Vendas vendas = dashboardService.vendas(
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"), "DIA");

        assertEquals(3, vendas.serie().size());
        assertEquals("2026-11-01", vendas.serie().get(0).periodo());
        assertEquals(0, vendas.serie().get(0).pedidos());
        assertEquals("2026-11-02", vendas.serie().get(1).periodo());
        assertEquals(1, vendas.serie().get(1).pedidos());
        assertEquals(0, new BigDecimal("45.50").compareTo(vendas.serie().get(1).faturamento()));
        assertEquals("2026-11-03", vendas.serie().get(2).periodo());
        assertEquals(0, vendas.serie().get(2).pedidos());
    }

    @Test
    void serieMensalAgrupaMesesSemPedidoEIncluiTodosOsMesesDoIntervalo() {
        pedidos = List.of(
                pedido("RECEBIDO", "20.00", "2026-11-01T03:00:00Z"),
                pedido("PRONTO", "30.00", "2026-12-01T03:00:00Z"));

        Vendas vendas = dashboardService.vendas(
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-31"), "MES");

        assertEquals(3, vendas.serie().size());
        assertEquals("2026-10", vendas.serie().get(0).periodo());
        assertEquals(0, vendas.serie().get(0).pedidos());
        assertEquals("2026-11", vendas.serie().get(1).periodo());
        assertEquals(1, vendas.serie().get(1).pedidos());
        assertEquals("2026-12", vendas.serie().get(2).periodo());
        assertEquals(1, vendas.serie().get(2).pedidos());
    }

    @Test
    void rejeitaIntervaloInvertidoSemConsultarBanco() {
        pedidos = List.of();
        assertThrows(RegraNegocioException.class, () -> dashboardService.resumo(
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-01")));
        assertEquals(0, consultas);
    }

    @Test
    void rankingUsaItensDosPedidosEExcluiPedidosCancelados() {
        Pedido venda = pedido("FINALIZADO", "120.00", "2026-11-02T15:00:00Z");
        ItemPedido item = new ItemPedido();
        item.setProdutoId("p1");
        item.setNomeProduto("Portuguesa");
        item.setQuantidade(2);
        item.setSubtotal(new BigDecimal("120.00"));
        venda.setItens(List.of(item));

        Pedido cancelado = pedido("CANCELADO", "300.00", "2026-11-03T15:00:00Z");
        ItemPedido itemCancelado = new ItemPedido();
        itemCancelado.setProdutoId("p2");
        itemCancelado.setNomeProduto("Calabresa");
        itemCancelado.setQuantidade(5);
        itemCancelado.setSubtotal(new BigDecimal("300.00"));
        cancelado.setItens(List.of(itemCancelado));
        pedidos = List.of(venda, cancelado);

        var ranking = dashboardService.produtosMaisVendidos(
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-05"), 5);

        assertEquals(1, ranking.produtos().size());
        assertEquals("Portuguesa", ranking.produtos().get(0).produto());
        assertEquals(2, ranking.produtos().get(0).quantidadeVendida());
        assertEquals(0, new BigDecimal("120.00").compareTo(ranking.produtos().get(0).receitaGerada()));
    }

    @Test
    void rankingIncluiPedidosLegadosSemListaDeItens() {
        Pedido legado = pedido("RECEBIDO", "40.00", "2026-11-02T15:00:00Z");
        legado.setSabor("Napolitana");
        legado.setTipoPizza("Tradicional");
        legado.setQuantidade(2);
        pedidos = List.of(legado);

        var ranking = dashboardService.produtosMaisVendidos(
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-05"), 5);

        assertEquals(1, ranking.produtos().size());
        assertEquals("Napolitana", ranking.produtos().get(0).produto());
        assertEquals(2, ranking.produtos().get(0).quantidadeVendida());
        assertEquals(0, new BigDecimal("40.00").compareTo(ranking.produtos().get(0).receitaGerada()));
    }

    private Pedido pedido(String status, String valor, String entrada) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(valor));
        pedido.setDataHoraEntrada(Instant.parse(entrada));
        return pedido;
    }
}
