package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResponse;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable String id) {
        return usuarioService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable String id, @RequestBody UsuarioRequest request) {
        return usuarioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
