package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            SessionRegistry sessionRegistry) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario salvarUsuario(Usuario recebido) {
        if (recebido == null || recebido.getUsername() == null || recebido.getUsername().isBlank()) {
            throw new RegraNegocioException("Informe o nome de usuário.");
        }
        String username = recebido.getUsername().trim();
        if (username.length() < 3 || username.length() > 80) {
            throw new RegraNegocioException("O nome de usuário deve ter entre 3 e 80 caracteres.");
        }

        PerfilUsuario perfil = converterPerfil(recebido.getRole());

        Optional<Usuario> comMesmoNome = usuarioRepository.findByUsername(username);
        if (comMesmoNome.isPresent() && !Objects.equals(comMesmoNome.get().getId(), recebido.getId())) {
            throw new RegraNegocioException("Esse nome de usuário já está cadastrado.");
        }

        if (recebido.getId() == null || recebido.getId().isBlank()) {
            validarSenha(recebido.getPassword(), true);
            Usuario novo = new Usuario();
            novo.setUsername(username);
            novo.setPassword(passwordEncoder.encode(recebido.getPassword()));
            novo.setRole(perfil.name());
            return usuarioRepository.save(novo);
        }

        Usuario existente = usuarioRepository.findById(recebido.getId())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        Authentication atual = SecurityContextHolder.getContext().getAuthentication();
        boolean editandoPropriaConta = atual != null && atual.getName().equals(existente.getUsername());
        PerfilUsuario perfilExistente = converterPerfilExistente(existente.getRole());
        if (editandoPropriaConta && !Objects.equals(perfilExistente, perfil)) {
            throw new RegraNegocioException("Não é permitido alterar o próprio perfil.");
        }
        boolean senhaAlterada = recebido.getPassword() != null && !recebido.getPassword().isBlank();
        if (senhaAlterada) {
            validarSenha(recebido.getPassword(), false);
        }

        String usernameAnterior = existente.getUsername();
        boolean acessoAlterado = !Objects.equals(usernameAnterior, username)
                || !Objects.equals(perfilExistente, perfil)
                || senhaAlterada;
        existente.setUsername(username);
        existente.setRole(perfil.name());
        if (senhaAlterada) {
            existente.setPassword(passwordEncoder.encode(recebido.getPassword()));
        }
        Usuario salvo = usuarioRepository.save(existente);
        if (acessoAlterado) {
            invalidarSessoes(usernameAnterior);
            if (!Objects.equals(usernameAnterior, username)) {
                invalidarSessoes(username);
            }
        }
        return salvo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        Authentication atual = SecurityContextHolder.getContext().getAuthentication();
        if (atual != null && atual.getName().equals(usuario.getUsername())) {
            throw new RegraNegocioException("Não é permitido excluir a própria conta.");
        }
        invalidarSessoes(usuario.getUsername());
        usuarioRepository.delete(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    private PerfilUsuario converterPerfil(String perfil) {
        try {
            return PerfilUsuario.fromPersisted(perfil);
        } catch (IllegalArgumentException exception) {
            throw new RegraNegocioException("Selecione um perfil válido.");
        }
    }

    private PerfilUsuario converterPerfilExistente(String perfil) {
        try {
            return PerfilUsuario.fromPersisted(perfil);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void validarSenha(String senha, boolean obrigatoria) {
        if (senha == null || senha.isBlank()) {
            if (obrigatoria) {
                throw new RegraNegocioException("Informe uma senha para o novo usuário.");
            }
            return;
        }
        if (senha.length() < 8) {
            throw new RegraNegocioException("A senha deve ter pelo menos 8 caracteres.");
        }
    }

    private void invalidarSessoes(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails userDetails && userDetails.getUsername().equals(username)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }
}
