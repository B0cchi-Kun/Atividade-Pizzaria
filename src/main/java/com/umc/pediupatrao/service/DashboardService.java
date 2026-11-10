package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.DashboardResponse;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.ProdutosMaisVendidos;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Resumo;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.StatusPedido;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.VendaPeriodo;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Vendas;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private static final ZoneId FUSO_OPERACAO = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DateTimeFormatter DATA_DIA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATA_MES = DateTimeFormatter.ofPattern("yyyy-MM");
    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    @Autowired
    public DashboardService(PedidoRepository pedidoRepository) {
        this(pedidoRepository, Clock.system(FUSO_OPERACAO));
    }

    DashboardService(PedidoRepository pedidoRepository, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Resumo resumo(LocalDate inicioSolicitado, LocalDate fimSolicitado) {
        Periodo periodo = resolverPeriodo(inicioSolicitado, fimSolicitado);
        List<Pedido> pedidos = buscarPedidos(periodo);
        Map<com.umc.pediupatrao.entity.StatusPedido, Long> contagens =
                new EnumMap<>(com.umc.pediupatrao.entity.StatusPedido.class);
        long outros = 0;
        long cancelados = 0;
        long concluidos = 0;
        long naoCancelados = 0;
        BigDecimal faturamento = ZERO;

        for (Pedido pedido : pedidos) {
            com.umc.pediupatrao.entity.StatusPedido status = statusNormalizado(pedido.getStatus());
            if (status == null) {
                outros++;
            } else {
                contagens.merge(status, 1L, Long::sum);
                if (status == com.umc.pediupatrao.entity.StatusPedido.CANCELADO) {
                    cancelados++;
                }
                if (status == com.umc.pediupatrao.entity.StatusPedido.FINALIZADO) {
                    concluidos++;
                }
            }
            if (status != com.umc.pediupatrao.entity.StatusPedido.CANCELADO) {
                naoCancelados++;
                faturamento = faturamento.add(valorSeguro(pedido.getValorTotal()));
            }
        }

        List<StatusPedido> porStatus = new ArrayList<>();
        for (com.umc.pediupatrao.entity.StatusPedido status : com.umc.pediupatrao.entity.StatusPedido.values()) {
            porStatus.add(new StatusPedido(status.name(), status.getDescricao(), contagens.getOrDefault(status, 0L)));
        }
        if (outros > 0) {
            porStatus.add(new StatusPedido("OUTROS", "Outros", outros));
        }

        faturamento = moeda(faturamento);
        BigDecimal ticket = naoCancelados == 0 ? ZERO
                : faturamento.divide(BigDecimal.valueOf(naoCancelados), 2, RoundingMode.HALF_UP);
        return new Resumo(periodo.inicio(), periodo.fim(), pedidos.size(), naoCancelados, concluidos,
                cancelados, faturamento, ticket, porStatus);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Vendas vendas(LocalDate inicioSolicitado, LocalDate fimSolicitado, String agrupamentoSolicitado) {
        Periodo periodo = resolverPeriodo(inicioSolicitado, fimSolicitado);
        Agrupamento agrupamento = Agrupamento.converter(agrupamentoSolicitado);
        validarTamanhoDoGrafico(periodo, agrupamento);

        Map<String, Acumulador> serie = criarSerieVazia(periodo, agrupamento);
        for (Pedido pedido : buscarPedidos(periodo)) {
            if (statusNormalizado(pedido.getStatus()) == com.umc.pediupatrao.entity.StatusPedido.CANCELADO) {
                continue;
            }
            String chave = chavePeriodo(pedido.getDataHoraEntrada(), agrupamento);
            Acumulador acumulador = serie.get(chave);
            if (acumulador != null) {
                acumulador.pedidos++;
                acumulador.valor = acumulador.valor.add(valorSeguro(pedido.getValorTotal()));
            }
        }

        List<VendaPeriodo> dados = serie.entrySet().stream()
                .map(entry -> new VendaPeriodo(entry.getKey(), entry.getValue().pedidos, moeda(entry.getValue().valor)))
                .toList();
        return new Vendas(periodo.inicio(), periodo.fim(), agrupamento.name(), dados);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ProdutosMaisVendidos produtosMaisVendidos(LocalDate inicioSolicitado, LocalDate fimSolicitado, Integer limiteSolicitado) {
        Periodo periodo = resolverPeriodo(inicioSolicitado, fimSolicitado);
        int limite = limiteSolicitado == null ? 5 : limiteSolicitado;
        if (limite < 1 || limite > 20) {
            throw new RegraNegocioException("O limite do ranking deve ficar entre 1 e 20.");
        }

        Map<String, AcumuladorProduto> ranking = new LinkedHashMap<>();
        for (Pedido pedido : buscarPedidos(periodo)) {
            if (statusNormalizado(pedido.getStatus()) == com.umc.pediupatrao.entity.StatusPedido.CANCELADO) {
                continue;
            }
            if (!pedido.getItens().isEmpty()) {
                for (ItemPedido item : pedido.getItens()) {
                    adicionarProduto(ranking, item.getProdutoId(), item.getNomeProduto(), item.getQuantidade(),
                            item.getSubtotal(), item.getPrecoUnitario());
                }
            } else if (pedido.getQuantidade() > 0) {
                String nomeLegado = primeiroTexto(pedido.getSabor(), pedido.getTipoPizza(), "Item legado");
                adicionarProduto(ranking, null, nomeLegado, pedido.getQuantidade(), pedido.getValorTotal(), null);
            }
        }

        List<ProdutoRanking> produtos = ranking.values().stream()
                .sorted(Comparator.comparingLong((AcumuladorProduto p) -> p.quantidade).reversed()
                        .thenComparing(p -> p.nome, String.CASE_INSENSITIVE_ORDER))
                .limit(limite)
                .map(p -> new ProdutoRanking(p.produtoId, p.nome, p.quantidade, moeda(p.receita)))
                .toList();
        return new ProdutosMaisVendidos(periodo.inicio(), periodo.fim(), produtos);
    }

    private Periodo resolverPeriodo(LocalDate inicioSolicitado, LocalDate fimSolicitado) {
        LocalDate fim = fimSolicitado == null ? LocalDate.now(clock) : fimSolicitado;
        LocalDate inicio = inicioSolicitado == null ? fim.minusDays(29) : inicioSolicitado;
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à data final.");
        }
        Instant instanteInicio = inicio.atStartOfDay(FUSO_OPERACAO).toInstant();
        Instant instanteFim = fim.plusDays(1).atStartOfDay(FUSO_OPERACAO).toInstant();
        return new Periodo(inicio, fim, instanteInicio, instanteFim);
    }

    private List<Pedido> buscarPedidos(Periodo periodo) {
        return pedidoRepository
                .findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThanOrderByDataHoraEntradaAsc(
                        periodo.inicioInclusivo(), periodo.fimExclusivo());
    }

    private Map<String, Acumulador> criarSerieVazia(Periodo periodo, Agrupamento agrupamento) {
        Map<String, Acumulador> serie = new LinkedHashMap<>();
        if (agrupamento == Agrupamento.DIA) {
            for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
                serie.put(dia.format(DATA_DIA), new Acumulador());
            }
        } else {
            for (YearMonth mes = YearMonth.from(periodo.inicio());
                    !mes.isAfter(YearMonth.from(periodo.fim())); mes = mes.plusMonths(1)) {
                serie.put(mes.format(DATA_MES), new Acumulador());
            }
        }
        return serie;
    }

    private String chavePeriodo(Instant instante, Agrupamento agrupamento) {
        LocalDate data = instante.atZone(FUSO_OPERACAO).toLocalDate();
        return agrupamento == Agrupamento.DIA
                ? data.format(DATA_DIA)
                : YearMonth.from(data).format(DATA_MES);
    }

    private void validarTamanhoDoGrafico(Periodo periodo, Agrupamento agrupamento) {
        if (agrupamento == Agrupamento.DIA && periodo.inicio().plusDays(366).isBefore(periodo.fim())) {
            throw new RegraNegocioException("Para períodos acima de 367 dias, selecione agrupamento mensal.");
        }
        if (agrupamento == Agrupamento.MES && YearMonth.from(periodo.inicio()).plusMonths(60)
                .isBefore(YearMonth.from(periodo.fim()))) {
            throw new RegraNegocioException("O gráfico mensal aceita períodos de até 61 meses.");
        }
    }

    private void adicionarProduto(Map<String, AcumuladorProduto> ranking, String id, String nome,
            int quantidade, BigDecimal subtotal, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            return;
        }
        String nomeSeguro = primeiroTexto(nome, id, "Produto sem nome");
        String chave = id == null || id.isBlank()
                ? "nome:" + nomeSeguro.trim().toLowerCase(Locale.ROOT)
                : "id:" + id;
        AcumuladorProduto produto = ranking.computeIfAbsent(chave, ignorado -> new AcumuladorProduto(id, nomeSeguro));
        produto.quantidade += quantidade;
        BigDecimal valor = subtotal;
        if (valor == null && precoUnitario != null) {
            valor = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
        if (valor != null) {
            produto.receita = produto.receita.add(valor);
        }
    }

    private com.umc.pediupatrao.entity.StatusPedido statusNormalizado(String status) {
        try {
            return com.umc.pediupatrao.entity.StatusPedido.fromPersisted(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String primeiroTexto(String primeiro, String segundo, String fallback) {
        if (primeiro != null && !primeiro.isBlank()) {
            return primeiro.trim();
        }
        if (segundo != null && !segundo.isBlank()) {
            return segundo.trim();
        }
        return fallback;
    }

    private enum Agrupamento {
        DIA, MES;

        private static Agrupamento converter(String value) {
            if (value == null || value.isBlank()) {
                return DIA;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new RegraNegocioException("O agrupamento deve ser DIA ou MES.");
            }
        }
    }

    private record Periodo(LocalDate inicio, LocalDate fim, Instant inicioInclusivo, Instant fimExclusivo) {}
    private static class Acumulador {
        private long pedidos;
        private BigDecimal valor = BigDecimal.ZERO;
    }
    private static class AcumuladorProduto {
        private final String produtoId;
        private final String nome;
        private long quantidade;
        private BigDecimal receita = BigDecimal.ZERO;
        private AcumuladorProduto(String produtoId, String nome) {
            this.produtoId = produtoId;
            this.nome = nome;
        }
    }
}
