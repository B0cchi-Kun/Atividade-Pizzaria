package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.CriarPedidoRequest;
import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {
    private static final BigDecimal LIMITE_DESCONTO = new BigDecimal("20.00");
    private static final BigDecimal CEM = new BigDecimal("100");
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(CriarPedidoRequest request) {
        if (request == null || request.getClienteId() == null || request.getClienteId().isBlank()) {
            throw new RegraNegocioException("Selecione um cliente para o pedido.");
        }
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
        if (request.getItens().isEmpty()) {
            throw new RegraNegocioException("Inclua ao menos um produto no pedido.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoRequest itemRequest : request.getItens()) {
            if (itemRequest == null) {
                throw new RegraNegocioException("Todos os itens devem informar um produto válido.");
            }
            String produtoId = itemRequest.getProdutoId();
            if (produtoId == null || produtoId.isBlank()) {
                continue;
            }
            if (itemRequest.getQuantidade() < 1 || itemRequest.getQuantidade() > 99) {
                throw new RegraNegocioException("A quantidade de cada item deve ficar entre 1 e 99.");
            }
            Produto produto = produtoRepository.findById(produtoId.trim())
                    .orElseThrow(() -> new RegraNegocioException("Um dos produtos selecionados não foi encontrado."));
            if (Boolean.FALSE.equals(produto.getAtivo())) {
                throw new RegraNegocioException("O produto " + produto.getNome() + " está inativo.");
            }
            if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() <= 0) {
                throw new RegraNegocioException("O produto " + produto.getNome() + " não tem preço válido.");
            }

            BigDecimal preco = moeda(BigDecimal.valueOf(produto.getPreco()));
            BigDecimal valorItem = moeda(preco.multiply(BigDecimal.valueOf(itemRequest.getQuantidade())));
            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(itemRequest.getQuantidade());
            item.setPrecoUnitario(preco);
            item.setSubtotal(valorItem);
            itens.add(item);
            subtotal = subtotal.add(valorItem);
        }
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Selecione ao menos um produto válido.");
        }

        String username = usuarioAtual();
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setItens(itens);
        pedido.setSubtotal(moeda(subtotal));
        pedido.setPercentualDesconto(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        pedido.setValorDesconto(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        pedido.setValorTotal(pedido.getSubtotal());
        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setRegistradoPor(username);
        pedido.setDataHoraEntrada(Instant.now());
        Pedido salvo = pedidoRepository.save(pedido);

        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("clienteId", salvo.getClienteId());
        depois.put("itens", itensAuditaveis(salvo.getItens()));
        depois.put("subtotal", salvo.getSubtotal());
        depois.put("percentualDesconto", salvo.getPercentualDesconto());
        depois.put("valorTotal", salvo.getValorTotal());
        depois.put("status", salvo.getStatus());
        depois.put("registradoPor", salvo.getRegistradoPor());
        depois.put("dataHoraEntrada", salvo.getDataHoraEntrada());
        auditoriaService.registrar("CRIACAO_PEDIDO", "Pedido", salvo.getId(), Map.of(), depois, null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido atualizarStatus(String id, String statusSolicitado) {
        Pedido pedido = buscarPedido(id);
        StatusPedido atual = statusAtual(pedido);
        StatusPedido proximo;
        try {
            proximo = StatusPedido.fromPersisted(statusSolicitado);
        } catch (IllegalArgumentException exception) {
            throw new RegraNegocioException("Status de pedido inválido.");
        }
        if (!atual.proximos().contains(proximo)) {
            throw new RegraNegocioException("Transição inválida de " + atual + " para " + proximo + ".");
        }
        if (proximo == StatusPedido.FINALIZADO
                && (pedido.getDataHoraSaida() == null || pedido.getResponsavelSaida() == null
                || pedido.getResponsavelSaida().isBlank())) {
            throw new RegraNegocioException("O pedido precisa ter saída registrada antes de ser finalizado.");
        }

        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("status", pedido.getStatus());
        antes.put("dataHoraSaida", pedido.getDataHoraSaida());
        antes.put("responsavelSaida", pedido.getResponsavelSaida());
        pedido.setStatus(proximo.name());
        if (proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO) {
            pedido.setDataHoraSaida(Instant.now());
            pedido.setResponsavelSaida(usuarioAtual());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("status", salvo.getStatus());
        depois.put("dataHoraSaida", salvo.getDataHoraSaida());
        depois.put("responsavelSaida", salvo.getResponsavelSaida());
        auditoriaService.registrar("ALTERACAO_STATUS_PEDIDO", "Pedido", id, antes, depois, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String justificativa) {
        Pedido pedido = buscarPedido(id);
        validarAntesDaSaida(pedido);
        if (pedido.getItens().isEmpty() || pedido.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O desconto exige um pedido com itens e valor calculado.");
        }
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) <= 0
                || percentual.compareTo(LIMITE_DESCONTO) > 0) {
            throw new RegraNegocioException("O desconto deve ser maior que zero e não pode passar de 20%.");
        }
        validarJustificativaOpcional(justificativa);

        BigDecimal percentualArredondado = moeda(percentual);
        BigDecimal valorDesconto = pedido.getSubtotal()
                .multiply(percentualArredondado)
                .divide(CEM, 2, RoundingMode.HALF_UP);
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("percentualDesconto", pedido.getPercentualDesconto());
        antes.put("valorDesconto", pedido.getValorDesconto());
        antes.put("valorTotal", pedido.getValorTotal());

        pedido.setPercentualDesconto(percentualArredondado);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(moeda(pedido.getSubtotal().subtract(valorDesconto)));
        Pedido salvo = pedidoRepository.save(pedido);
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("percentualDesconto", salvo.getPercentualDesconto());
        depois.put("valorDesconto", salvo.getValorDesconto());
        depois.put("valorTotal", salvo.getValorTotal());
        auditoriaService.registrar("APLICACAO_DESCONTO", "Pedido", id, antes, depois, justificativa);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        validarAntesDaSaida(pedido);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraNegocioException("Informe a justificativa do cancelamento.");
        }
        if (justificativa.trim().length() > 500) {
            throw new RegraNegocioException("A justificativa deve ter no máximo 500 caracteres.");
        }

        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("status", pedido.getStatus());
        antes.put("canceladoPor", pedido.getCanceladoPor());
        antes.put("dataHoraCancelamento", pedido.getDataHoraCancelamento());
        antes.put("justificativaCancelamento", pedido.getJustificativaCancelamento());
        String username = usuarioAtual();
        Instant agora = Instant.now();
        pedido.setStatus(StatusPedido.CANCELADO.name());
        pedido.setCanceladoPor(username);
        pedido.setDataHoraCancelamento(agora);
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido);
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("status", salvo.getStatus());
        depois.put("canceladoPor", salvo.getCanceladoPor());
        depois.put("dataHoraCancelamento", salvo.getDataHoraCancelamento());
        depois.put("justificativaCancelamento", salvo.getJustificativaCancelamento());
        auditoriaService.registrar("CANCELAMENTO_PEDIDO", "Pedido", id, antes, depois, justificativa.trim());
        return salvo;
    }

    public List<StatusPedido> proximosStatus(String status) {
        return statusAtual(status).proximos();
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Pedido não encontrado."));
    }

    private StatusPedido statusAtual(Pedido pedido) {
        return statusAtual(pedido.getStatus());
    }

    private StatusPedido statusAtual(String status) {
        try {
            return StatusPedido.fromPersisted(status);
        } catch (IllegalArgumentException exception) {
            throw new RegraNegocioException("O pedido possui um status inválido e precisa ser revisado.");
        }
    }

    private void validarAntesDaSaida(Pedido pedido) {
        StatusPedido status = statusAtual(pedido);
        if (!status.permiteDescontoOuCancelamento() || pedido.getDataHoraSaida() != null) {
            throw new RegraNegocioException("Descontos e cancelamentos só podem ocorrer antes da saída do pedido.");
        }
    }

    private void validarJustificativaOpcional(String justificativa) {
        if (justificativa != null && justificativa.trim().length() > 500) {
            throw new RegraNegocioException("A justificativa deve ter no máximo 500 caracteres.");
        }
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuário autenticado não encontrado.");
        }
        return authentication.getName();
    }

    private List<Map<String, Object>> itensAuditaveis(List<ItemPedido> itens) {
        return itens.stream().map(item -> {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("produtoId", item.getProdutoId());
            linha.put("nomeProduto", item.getNomeProduto());
            linha.put("quantidade", item.getQuantidade());
            linha.put("precoUnitario", item.getPrecoUnitario());
            linha.put("subtotal", item.getSubtotal());
            return linha;
        }).toList();
    }
}


