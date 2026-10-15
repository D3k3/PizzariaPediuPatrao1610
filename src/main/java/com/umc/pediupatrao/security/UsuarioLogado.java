package com.umc.pediupatrao.security;

import com.umc.pediupatrao.entity.Perfil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;

public final class UsuarioLogado {
    private static final String PREFIXO_ROLE = "ROLE_";

    private UsuarioLogado() {
    }

    public static Optional<String> username() {
        return autenticacao().map(Authentication::getName);
    }

    public static Optional<Perfil> perfil() {
        return autenticacao().flatMap(auth -> auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith(PREFIXO_ROLE))
                .map(a -> a.substring(PREFIXO_ROLE.length()))
                .flatMap(nome -> Arrays.stream(Perfil.values()).filter(p -> p.name().equals(nome)))
                .findFirst());
    }

    public static String exigirUsername() {
        return username().orElseThrow(() -> new IllegalStateException("Operação exige usuário autenticado."));
    }

    private static Optional<Authentication> autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }
}
