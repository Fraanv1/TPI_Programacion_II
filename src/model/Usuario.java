/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.time.LocalDateTime;

/**
 *
 * @author leanf
 */
public class Usuario extends Base {
    private String username;
    private String email;
    private boolean activo;
    private java.time.LocalDateTime fechaRegistro;
    private CredencialAcceso credencial;
    
    // Constructor vacio
    public Usuario() {
        super();
        this.fechaRegistro = LocalDateTime.now();
    }

    // Constructor completo
    public Usuario(Long id, String username, String email, boolean activo, CredencialAcceso credencial) {
        super(id, false);
        this.username = username;
        this.email = email;
        this.activo = activo;
        this.fechaRegistro = LocalDateTime.now();
        this.credencial = credencial;
    }
    
   // Solo los parametros not null que no tienen metodo default en SQL 
    // (ademas de eliminado = false y la fecha de registro, que tienen metodos default en SQL
    // pero por comodidad los ponemos también acá sin que el usuario los ingrese)
    public Usuario(String username, String email, CredencialAcceso credencial) {
        super();
        this.username = username;
        this.email = email;
        this.fechaRegistro = LocalDateTime.now();
        this.credencial = credencial;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
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

    @Override
    public String toString() {
        return "Usuario{" + "id=" + getId() +", eliminado=" + isEliminado() + ", username=" + username + ", email=" + email + ", activo=" + activo + ", fechaRegistro=" + fechaRegistro + ", credencial=" + credencial + '}';
    }
    
}
