package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResponse;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras do UsuarioService com Mockito puro (sem Spring, sem MongoDB). */
class UsuarioServiceTest {

    private UsuarioRepository repo;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenAnswer(i -> "HASH(" + i.getArgument(0) + ")");
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        service = new UsuarioService(repo, encoder);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void logarComo(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
    }

    private Usuario usuario(String id, String username, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("HASH-ANTIGO");
        u.setRole(perfil);
        return u;
    }

    private UsuarioRequest request(String username, String senha, Perfil perfil) {
        UsuarioRequest r = new UsuarioRequest();
        r.setUsername(username);
        r.setPassword(senha);
        r.setPerfil(perfil);
        return r;
    }

    // ---------- criar ----------

    @Test
    void criarCodificaASenhaENuncaGuardaEmTextoClaro() {
        when(repo.findAllByUsernameIgnoreCase("maria")).thenReturn(List.of());

        UsuarioResponse resposta = service.criar(request("  maria  ", "senha1234", Perfil.GERENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertEquals("maria", captor.getValue().getUsername());
        assertEquals("HASH(senha1234)", captor.getValue().getPassword());
        assertNotEquals("senha1234", captor.getValue().getPassword());
        assertEquals(Perfil.GERENTE, resposta.getPerfil());
    }

    @Test
    void criarRejeitaUsernameDuplicadoDiferindoSoEmMaiusculas() {
        when(repo.findAllByUsernameIgnoreCase("MARIA")).thenReturn(List.of(usuario("1", "maria", Perfil.ATENDENTE)));

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> service.criar(request("MARIA", "senha1234", Perfil.ATENDENTE)));

        assertEquals("Já existe um usuário com esse username.", e.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void criarRejeitaSenhaCurta() {
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(request("maria", "curta", Perfil.ATENDENTE)));
        verify(repo, never()).save(any());
    }

    @Test
    void criarRejeitaSenhaAusente() {
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(request("maria", null, Perfil.ATENDENTE)));
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(request("maria", "", Perfil.ATENDENTE)));
        verify(repo, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizarSemSenhaMantemOHashAtual() {
        logarComo("admin");
        when(repo.findById("2")).thenReturn(Optional.of(usuario("2", "maria", Perfil.ATENDENTE)));
        when(repo.findAllByUsernameIgnoreCase("maria")).thenReturn(List.of(usuario("2", "maria", Perfil.ATENDENTE)));

        service.atualizar("2", request("maria", "", Perfil.GERENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertEquals("HASH-ANTIGO", captor.getValue().getPassword());
        assertEquals(Perfil.GERENTE, captor.getValue().getRole());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void naoPermiteAlterarOProprioPerfil() {
        logarComo("admin");
        when(repo.findById("1")).thenReturn(Optional.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.findAllByUsernameIgnoreCase("admin")).thenReturn(List.of(usuario("1", "admin", Perfil.ADMIN)));

        assertThrows(RegraNegocioException.class,
                () -> service.atualizar("1", request("admin", null, Perfil.GERENTE)));
        verify(repo, never()).save(any());
    }

    @Test
    void naoPermiteAlterarOProprioUsername() {
        logarComo("admin");
        when(repo.findById("1")).thenReturn(Optional.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.findAllByUsernameIgnoreCase("chefe")).thenReturn(List.of());

        assertThrows(RegraNegocioException.class,
                () -> service.atualizar("1", request("chefe", null, Perfil.ADMIN)));
        verify(repo, never()).save(any());
    }

    @Test
    void naoPermiteRebaixarOUltimoAdmin() {
        logarComo("outro.logado");
        when(repo.findById("1")).thenReturn(Optional.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.findAllByUsernameIgnoreCase("admin")).thenReturn(List.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.countByRole(Perfil.ADMIN)).thenReturn(1L);

        assertThrows(RegraNegocioException.class,
                () -> service.atualizar("1", request("admin", null, Perfil.ATENDENTE)));
        verify(repo, never()).save(any());
    }

    @Test
    void comDoisAdminsPermiteRebaixarOOutro() {
        logarComo("admin");
        when(repo.findById("2")).thenReturn(Optional.of(usuario("2", "chefe", Perfil.ADMIN)));
        when(repo.findAllByUsernameIgnoreCase("chefe")).thenReturn(List.of(usuario("2", "chefe", Perfil.ADMIN)));
        when(repo.countByRole(Perfil.ADMIN)).thenReturn(2L);

        UsuarioResponse resposta = service.atualizar("2", request("chefe", null, Perfil.ATENDENTE));

        assertEquals(Perfil.ATENDENTE, resposta.getPerfil());
    }

    // ---------- excluir ----------

    @Test
    void naoPermiteExcluirASiMesmo() {
        logarComo("admin");
        when(repo.findById("1")).thenReturn(Optional.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.countByRole(Perfil.ADMIN)).thenReturn(2L);

        assertThrows(RegraNegocioException.class, () -> service.excluir("1"));
        verify(repo, never()).deleteById(anyString());
    }

    @Test
    void naoPermiteExcluirOUltimoAdmin() {
        logarComo("gerente.logado");
        when(repo.findById("1")).thenReturn(Optional.of(usuario("1", "admin", Perfil.ADMIN)));
        when(repo.countByRole(Perfil.ADMIN)).thenReturn(1L);

        assertThrows(RegraNegocioException.class, () -> service.excluir("1"));
        verify(repo, never()).deleteById(anyString());
    }

    @Test
    void semAutenticacaoNoContextoNenhumaRegraDeSiMesmoSeAplica() {
        // Contexto vazio: a regra "de si mesmo" não se aplica; só vale a do último ADMIN
        when(repo.findById("3")).thenReturn(Optional.of(usuario("3", "joao", Perfil.ATENDENTE)));

        service.excluir("3");

        verify(repo).deleteById("3");
    }
}
