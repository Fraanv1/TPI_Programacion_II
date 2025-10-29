/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import java.time.LocalDateTime;

/**
 *
 * @author lu
 */
public class CredencialAcceso {
    private Long id;
    private Boolean eliminado;
    private String email;
    private Boolean activo;
    private java.time.LocalDateTime fechaRegistro;
    
    private CredencialAcceso credencial;

    public CredencialAcceso() {
    }

    public CredencialAcceso(Long id, Boolean eliminado, String email, Boolean activo, LocalDateTime fechaRegistro, CredencialAcceso credencial) {
        this.id = id;
        this.eliminado = eliminado;
        this.email = email;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
        this.credencial = credencial;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEliminado() {
        return eliminado;
    }

    public void setEliminado(Boolean eliminado) {
        this.eliminado = eliminado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public CredencialAcceso getCredencial() {
        return credencial;
    }

    public void setCredencial(CredencialAcceso credencial) {
        this.credencial = credencial;
    }
    
    
    
}
