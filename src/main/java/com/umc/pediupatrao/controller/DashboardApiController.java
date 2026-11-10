package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.DashboardResponse.ProdutosMaisVendidos;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Resumo;
import com.umc.pediupatrao.dto.dashboard.DashboardResponse.Vendas;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardApiController {
    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public Resumo resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboardService.resumo(inicio, fim);
    }

    @GetMapping("/vendas")
    public Vendas vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false, defaultValue = "DIA") String agrupamento) {
        return dashboardService.vendas(inicio, fim, agrupamento);
    }

    @GetMapping("/produtos-mais-vendidos")
    public ProdutosMaisVendidos produtosMaisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Integer limite) {
        return dashboardService.produtosMaisVendidos(inicio, fim, limite);
    }
}
