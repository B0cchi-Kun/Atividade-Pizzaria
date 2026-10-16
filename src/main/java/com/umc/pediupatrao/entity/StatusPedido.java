package com.umc.pediupatrao.entity;

import java.util.List;
import java.util.Locale;

public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusPedido fromPersisted(String value) {
        if (value == null || value.isBlank()) {
            return RECEBIDO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "PENDENTE", "NOVO", "AGUARDANDO" -> RECEBIDO;
            case "EM_PREPARO", "PREPARANDO" -> EM_PREPARACAO;
            case "ENTREGUE", "CONCLUIDO", "CONCLUÍDO" -> FINALIZADO;
            case "SAIU_PARA_ENTREGA", "EM_ENTREGA" -> SAIU_PARA_ENTREGA;
            default -> valueOf(normalized);
        };
    }

    public List<StatusPedido> proximos() {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> List.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public boolean permiteDescontoOuCancelamento() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
