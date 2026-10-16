package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResponse;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UsuarioService {
    private static final Pattern USERNAME_VALIDO = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final int SENHA_MIN = 8;
    private static final int SENHA_MAX_BYTES = 72;

    static final String CAMPO_CREDENCIAL = "credencial";
    static final String CREDENCIAL_ANTES = "***";
    static final String CREDENCIAL_DEPOIS = "*** alterada";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse buscar(String id) {
        return paraResponse(obter(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse criar(UsuarioRequest request) {
        String username = validarUsername(request.getUsername(), null);
        validarSenha(request.getPassword(), true);
        validarPerfil(request.getPerfil());

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRole(request.getPerfil());
        Usuario salvo = usuarioRepository.save(usuario);
        auditoriaService.registrar(OperacaoAuditoria.CRIACAO_USUARIO, EntidadeAuditada.USUARIO, salvo.getId(),
                List.of(new AlteracaoCampo("username", null, salvo.getUsername()),
                        new AlteracaoCampo("role", null, salvo.getRole().name())),
                null);
        return paraResponse(salvo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse atualizar(String id, UsuarioRequest request) {
        Usuario existente = obter(id);

        String username = validarUsername(request.getUsername(), id);
        validarSenha(request.getPassword(), false);
        validarPerfil(request.getPerfil());

        if (ehUsuarioLogado(existente)) {
            if (!Objects.equals(existente.getRole(), request.getPerfil())) {
                throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
            }

            if (!Objects.equals(existente.getUsername(), username)) {
                throw new RegraNegocioException("Você não pode alterar o seu próprio username.");
            }
        }
        if (existente.getRole() == Perfil.ADMIN && request.getPerfil() != Perfil.ADMIN
                && ehUltimoAdmin()) {
            throw new RegraNegocioException("Não é possível rebaixar o último ADMIN do sistema.");
        }

        boolean trocouSenha = request.getPassword() != null && !request.getPassword().isEmpty();
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        if (!Objects.equals(existente.getUsername(), username)) {
            alteracoes.add(new AlteracaoCampo("username", existente.getUsername(), username));
        }
        if (existente.getRole() != request.getPerfil()) {
            alteracoes.add(new AlteracaoCampo("role", ComparadorCampos.formatar(existente.getRole()),
                    request.getPerfil().name()));
        }
        if (trocouSenha) {
            alteracoes.add(new AlteracaoCampo(CAMPO_CREDENCIAL, CREDENCIAL_ANTES, CREDENCIAL_DEPOIS));
        }

        existente.setUsername(username);
        existente.setRole(request.getPerfil());
        if (trocouSenha) {
            existente.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        Usuario salvo = usuarioRepository.save(existente);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(OperacaoAuditoria.ALTERACAO_USUARIO, EntidadeAuditada.USUARIO, salvo.getId(),
                    alteracoes, null);
        }
        return paraResponse(salvo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String id) {
        Usuario existente = obter(id);

        if (ehUsuarioLogado(existente)) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário.");
        }
        if (existente.getRole() == Perfil.ADMIN && ehUltimoAdmin()) {
            throw new RegraNegocioException("Não é possível excluir o último ADMIN do sistema.");
        }
        usuarioRepository.deleteById(id);
        auditoriaService.registrar(OperacaoAuditoria.EXCLUSAO_USUARIO, EntidadeAuditada.USUARIO, id,
                List.of(new AlteracaoCampo("username", existente.getUsername(), null)), null);
    }

    private Usuario obter(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado."));
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRole());
    }

    private boolean ehUltimoAdmin() {
        return usuarioRepository.countByRole(Perfil.ADMIN) <= 1;
    }

    private boolean ehUsuarioLogado(Usuario alvo) {
        return UsuarioLogado.username().map(nome -> nome.equals(alvo.getUsername())).orElse(false);
    }

    private String validarUsername(String bruto, String idAtual) {
        String username = bruto == null ? "" : bruto.trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("O username é obrigatório.");
        }
        if (!USERNAME_VALIDO.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "O username deve ter de 3 a 30 caracteres e usar apenas letras, números, ponto, hífen e sublinhado.");
        }
        boolean duplicado = usuarioRepository.findAllByUsernameIgnoreCase(username).stream()
                .anyMatch(u -> idAtual == null || !idAtual.equals(u.getId()));
        if (duplicado) {
            throw new RegraNegocioException("Já existe um usuário com esse username.");
        }
        return username;
    }

    private void validarSenha(String senha, boolean obrigatoria) {
        boolean vazia = senha == null || senha.isEmpty();
        if (vazia) {
            if (obrigatoria) {
                throw new IllegalArgumentException("A senha é obrigatória.");
            }
            return;
        }
        if (senha.length() < SENHA_MIN) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new IllegalArgumentException("A senha deve ter no máximo 72 bytes.");
        }
    }

    private void validarPerfil(Perfil perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil é obrigatório.");
        }
    }
}
