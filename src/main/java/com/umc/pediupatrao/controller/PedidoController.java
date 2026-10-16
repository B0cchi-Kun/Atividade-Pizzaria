package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoPedidoRequest;
import com.umc.pediupatrao.dto.CriarPedidoRequest;
import com.umc.pediupatrao.dto.DescontoPedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public ResponseEntity<Pedido> criarPedido(@RequestBody CriarPedidoRequest request) {
        return ResponseEntity.status(201).body(pedidoService.criarPedido(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, status);
    }

    @PostMapping("/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoPedidoRequest request) {
        return pedidoService.aplicarDesconto(id, request.getPercentual(), request.getJustificativa());
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(@PathVariable String id, @RequestBody CancelamentoPedidoRequest request) {
        return pedidoService.cancelar(id, request.getJustificativa());
    }
}
