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
    private String hashPassword;
    private String salt;
    private java.time.LocalDateTime ultimoCambio;
    private Boolean rLocalDateTimeequireReset;

    public CredencialAcceso() {
    }

    public CredencialAcceso(Long id, Boolean eliminado, String hashPassword, String salt, LocalDateTime ultimoCambio, Boolean rLocalDateTimeequireReset) {
        this.id = id;
        this.eliminado = eliminado;
        this.hashPassword = hashPassword;
        this.salt = salt;
        this.ultimoCambio = ultimoCambio;
        this.rLocalDateTimeequireReset = rLocalDateTimeequireReset;
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

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public LocalDateTime getUltimoCambio() {
        return ultimoCambio;
    }

    public void setUltimoCambio(LocalDateTime ultimoCambio) {
        this.ultimoCambio = ultimoCambio;
    }

    public Boolean getrLocalDateTimeequireReset() {
        return rLocalDateTimeequireReset;
    }

    public void setrLocalDateTimeequireReset(Boolean rLocalDateTimeequireReset) {
        this.rLocalDateTimeequireReset = rLocalDateTimeequireReset;
    }



}
