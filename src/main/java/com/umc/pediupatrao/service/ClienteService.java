package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente dados) {
        validar(dados);
        return dados.getId() == null ? criar(dados) : atualizar(dados);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Cliente atual = obter(id);
        clienteRepository.deleteById(id);

        auditoriaService.registrar(OperacaoAuditoria.EXCLUSAO_CLIENTE, EntidadeAuditada.CLIENTE, id,
                List.of(new AlteracaoCampo("nome", atual.getNome(), null)), null);
    }

    private Cliente criar(Cliente dados) {
        Cliente novo = new Cliente();
        copiarCamposEditaveis(dados, novo);
        Cliente salvo = clienteRepository.save(novo);
        auditoriaService.registrar(OperacaoAuditoria.CRIACAO_CLIENTE, EntidadeAuditada.CLIENTE, salvo.getId(),
                ComparadorCampos.comparar(null, salvo), null);
        return salvo;
    }

    private Cliente atualizar(Cliente dados) {
        Cliente atual = obter(dados.getId());
        Cliente antes = atual.copia();

        copiarCamposEditaveis(dados, atual);
        List<AlteracaoCampo> alteracoes = ComparadorCampos.comparar(antes, atual);
        if (alteracoes.isEmpty()) {
            return atual;
        }
        Cliente salvo = clienteRepository.save(atual);
        auditoriaService.registrar(OperacaoAuditoria.ALTERACAO_CLIENTE, EntidadeAuditada.CLIENTE, salvo.getId(),
                alteracoes, null);
        return salvo;
    }

    private Cliente obter(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado."));
    }

    private void validar(Cliente dados) {
        if (dados == null || limpar(dados.getNome()) == null) {
            throw new IllegalArgumentException("O nome do cliente é obrigatório.");
        }
    }

    private static void copiarCamposEditaveis(Cliente origem, Cliente destino) {
        destino.setNome(limpar(origem.getNome()));
        destino.setTelefone(limpar(origem.getTelefone()));
        destino.setCep(limpar(origem.getCep()));
        destino.setLogradouro(limpar(origem.getLogradouro()));
        destino.setNumero(limpar(origem.getNumero()));
        destino.setComplemento(limpar(origem.getComplemento()));
        destino.setBairro(limpar(origem.getBairro()));
        destino.setCidade(limpar(origem.getCidade()));
        destino.setEstado(limpar(origem.getEstado()));
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}
