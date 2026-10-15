package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        criarSeNaoExistir("admin", "teste123", Perfil.ADMIN);
        criarSeNaoExistir("gerente", "gerente123", Perfil.GERENTE);
        criarSeNaoExistir("atendente", "atendente123", Perfil.ATENDENTE);
    }

    private void criarSeNaoExistir(String username, String senha, Perfil perfil) {
        if (repo.findByUsername(username).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(senha));
        usuario.setRole(perfil);
        repo.save(usuario);
        log.info("Usuário de demonstração '{}' criado com perfil {}", username, perfil);
    }
}
