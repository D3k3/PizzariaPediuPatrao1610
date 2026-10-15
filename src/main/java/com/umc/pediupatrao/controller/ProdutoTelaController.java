package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
public class ProdutoTelaController {
    private final ProdutoService produtoService;

    public ProdutoTelaController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping("/produtos")
    public String listar(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @GetMapping("/produtos/novo")
    public String novo(Model model) {
        Produto produto = new Produto();
        produto.setAtivo(true);
        return formulario(produto, model);
    }

    @GetMapping("/produtos/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes redirect) {
        try {
            return formulario(produtoService.buscar(id), model);
        } catch (NoSuchElementException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/produtos";
        }
    }

    @PostMapping("/produtos/salvar")
    public String criar(@ModelAttribute("produto") Produto produto, RedirectAttributes redirect) {
        produto.setId(null);
        return salvar(produto, "Produto criado com sucesso!", "redirect:/produtos/novo", redirect);
    }

    @PostMapping("/produtos/editar/{id}")
    public String atualizar(@PathVariable String id, @ModelAttribute("produto") Produto produto,
            RedirectAttributes redirect) {
        produto.setId(id);
        return salvar(produto, "Produto atualizado com sucesso!", "redirect:/produtos/editar/" + id, redirect);
    }

    @PostMapping("/produtos/excluir/{id}")
    public String excluir(@PathVariable String id, RedirectAttributes redirect) {
        try {
            produtoService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Produto removido com sucesso!");
        } catch (NoSuchElementException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }

    private String formulario(Produto produto, Model model) {
        model.addAttribute("produto", produto);
        model.addAttribute("content", "produtos/produto-form :: content");
        return "layout";
    }

    private String salvar(Produto produto, String mensagem, String voltarEmErro, RedirectAttributes redirect) {
        try {
            produtoService.salvar(produto);
            redirect.addFlashAttribute("sucesso", mensagem);
            return "redirect:/produtos";
        } catch (IllegalArgumentException | NoSuchElementException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return voltarEmErro;
        }
    }
}
