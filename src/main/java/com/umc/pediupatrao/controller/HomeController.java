package com.umc.pediupatrao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResponse;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuarioForm", new UsuarioRequest());
        model.addAttribute("usuarioId", null);
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            UsuarioResponse usuario = usuarioService.buscar(id);
            UsuarioRequest form = new UsuarioRequest();
            form.setUsername(usuario.getUsername());
            form.setPerfil(usuario.getPerfil());

            model.addAttribute("usuarioForm", form);
            model.addAttribute("usuarioId", usuario.getId());
            model.addAttribute("perfis", Perfil.values());
            model.addAttribute("content", "usuarios/form :: content");
            return "layout";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute("usuarioForm") UsuarioRequest form,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.criar(form);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException | RegraNegocioException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/novo";
        }
    }

    @PostMapping("/usuarios/editar/{id}")
    public String atualizarUsuario(@PathVariable String id,
            @ModelAttribute("usuarioForm") UsuarioRequest form,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.atualizar(id, form);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException | RegraNegocioException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
    }

    @PostMapping("/usuarios/deletar/{id}")
    public String deletarUsuario(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (IllegalArgumentException | RegraNegocioException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        List<Cliente> clientes = clienteService.listarClientes();
        model.addAttribute("clientes", clientes);
        model.addAttribute("content", "clientes/lista :: content");
        log.info("Carregando fragmento: clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        cliente.setId(null);
        try {
            clienteService.salvar(cliente);
            redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
            return "redirect:/clientes";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/clientes/novo";
        }
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }

        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id,
            @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        try {
            clienteService.salvar(cliente);
            redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
            return "redirect:/clientes";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/clientes/editar/" + id;
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/clientes";
        }
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }
}
