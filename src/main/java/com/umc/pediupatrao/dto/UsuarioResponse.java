package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;

public class UsuarioResponse {
    private String id;
    private String username;
    private Perfil perfil;

    public UsuarioResponse() {
    }

    public UsuarioResponse(String id, String username, Perfil perfil) {
        this.id = id;
        this.username = username;
        this.perfil = perfil;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }
}
