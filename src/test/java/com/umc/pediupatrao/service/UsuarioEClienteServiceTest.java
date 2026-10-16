package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioEClienteServiceTest {

    private final AuditoriaRepository eventos = mock(AuditoriaRepository.class);
    private final AuditoriaService auditoria = new AuditoriaService(eventos, Clock.systemDefaultZone());

    private void logarComo(String username, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "x", List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void alterarTelefoneGeraAuditoriaComValorAnteriorENovo() {
        logarComo("atendente", "ATENDENTE");
        ClienteRepository clientes = mock(ClienteRepository.class);
        Cliente atual = new Cliente();
        atual.setId("c1");
        atual.setNome("Ana");
        atual.setTelefone("11987654321");
        when(clientes.findById("c1")).thenReturn(Optional.of(atual));
        when(clientes.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Cliente editado = new Cliente();
        editado.setId("c1");
        editado.setNome("Ana");
        editado.setTelefone("11912345678");
        new ClienteService(clientes, auditoria).salvar(editado);

        ArgumentCaptor<AuditoriaEvento> evento = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(eventos).save(evento.capture());
        assertEquals(OperacaoAuditoria.ALTERACAO_CLIENTE, evento.getValue().getOperacao());
        assertEquals(List.of(new AlteracaoCampo("telefone", "11987654321", "11912345678")),
                evento.getValue().getAlteracoes());
    }

    @Test
    void adminNaoPodeTrocarOProprioPerfil() {
        logarComo("admin", "ADMIN");
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario admin = new Usuario();
        admin.setId("u1");
        admin.setUsername("admin");
        admin.setRole(Perfil.ADMIN);
        when(usuarios.findById("u1")).thenReturn(Optional.of(admin));
        when(usuarios.findAllByUsernameIgnoreCase(anyString())).thenReturn(List.of(admin));
        UsuarioService service = new UsuarioService(usuarios, mock(PasswordEncoder.class), auditoria);

        UsuarioRequest pedido = new UsuarioRequest();
        pedido.setUsername("admin");
        pedido.setPerfil(Perfil.GERENTE);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.atualizar("u1", pedido));
        assertEquals("Você não pode alterar o seu próprio perfil.", erro.getMessage());
        verify(usuarios, never()).save(any());
    }
}
