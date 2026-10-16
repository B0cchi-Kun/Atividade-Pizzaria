package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsername(username)
                .map(usuario -> {
                    try {
                        PerfilUsuario perfil = PerfilUsuario.fromPersisted(usuario.getRole());
                        return User.withUsername(usuario.getUsername())
                                .password(usuario.getPassword())
                                .roles(perfil.name())
                                .build();
                    } catch (IllegalArgumentException exception) {
                        throw new UsernameNotFoundException("Usuário sem perfil autorizado.");
                    }
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
