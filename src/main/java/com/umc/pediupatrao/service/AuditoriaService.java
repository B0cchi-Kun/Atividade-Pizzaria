package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public EventoAuditoria registrar(String operacao, String entidade, String entidadeId,
            Map<String, Object> anteriores, Map<String, Object> novos, String justificativa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Não foi possível identificar o usuário da operação.");
        }

        String username = authentication.getName();
        String perfil = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse("DESCONHECIDO");

        EventoAuditoria evento = new EventoAuditoria();
        evento.setUsuarioId(usuarioRepository.findByUsername(username).map(usuario -> usuario.getId()).orElse(null));
        evento.setUsuario(username);
        evento.setPerfil(perfil);
        evento.setDataHora(Instant.now());
        evento.setTipoOperacao(operacao);
        evento.setEntidade(entidade);
        evento.setEntidadeId(entidadeId);
        evento.setValoresAnteriores(anteriores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(anteriores));
        evento.setValoresNovos(novos == null ? new LinkedHashMap<>() : new LinkedHashMap<>(novos));
        evento.setJustificativa(justificativa);
        return auditoriaRepository.save(evento);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<EventoAuditoria> listarRecentes() {
        return auditoriaRepository.findTop500ByOrderByDataHoraDesc();
    }
}
