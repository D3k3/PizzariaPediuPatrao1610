package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarAtivos() {
        return produtoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public Produto buscar(String id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado."));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new NoSuchElementException("Produto não encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto produto) {
        validar(produto);
        if (produto.getId() != null && !produtoRepository.existsById(produto.getId())) {
            throw new NoSuchElementException("Produto não encontrado para atualização.");
        }
        produto.setNome(produto.getNome().trim());
        produto.setAtivo(Boolean.TRUE.equals(produto.getAtivo()));
        return produtoRepository.save(produto);
    }

    private void validar(Produto produto) {
        if (produto == null || produto.getNome() == null || produto.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
    }
}
