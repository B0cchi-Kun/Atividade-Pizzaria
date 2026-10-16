package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner inicializarAdministrador(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.password:}") String password) {
        return args -> {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return;
            }
            Usuario administrador = usuarios.findByUsername(username.trim()).orElseGet(Usuario::new);
            boolean novo = administrador.getId() == null;
            administrador.setUsername(username.trim());
            administrador.setRole("ADMIN");
            if (novo) {
                administrador.setPassword(passwordEncoder.encode(password));
                usuarios.save(administrador);
            } else if (administrador.getPassword() == null || administrador.getPassword().isBlank()) {
                administrador.setPassword(passwordEncoder.encode(password));
                usuarios.save(administrador);
            } else if (!"ADMIN".equalsIgnoreCase(administrador.getRole())) {
                usuarios.save(administrador);
            }
        };
    }
}
