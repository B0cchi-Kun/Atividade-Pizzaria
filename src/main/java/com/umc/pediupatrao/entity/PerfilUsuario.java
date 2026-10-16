package com.umc.pediupatrao.entity;

import java.util.Locale;

public enum PerfilUsuario {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static PerfilUsuario fromPersisted(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O usuário não possui um perfil válido.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("ROLE_", "");
        if ("USER".equals(normalized)) {
            return ATENDENTE;
        }
        return valueOf(normalized);
    }
}
