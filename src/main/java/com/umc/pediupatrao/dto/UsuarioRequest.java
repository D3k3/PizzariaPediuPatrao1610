package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;

public class UsuarioRequest {
    private String username;
    private String password;
    private Perfil perfil;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }
}
