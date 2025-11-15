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
public class CredencialAcceso extends Base {
    private String hashPassword;
    private String salt;
    private java.time.LocalDateTime ultimoCambio;
    private boolean requireReset;

    public CredencialAcceso() {
        super();
    }

    public CredencialAcceso(Long id, String hashPassword, String salt, boolean requireReset) {
        super(id, false);
        this.hashPassword = hashPassword;
        this.salt = salt;
        this.ultimoCambio = LocalDateTime.now();
        this.requireReset = requireReset;
    }
    
    // Solo los parametros not null que no tienen metodo default en SQL 
    // (ademas de eliminado = false y el último cambio, que tienen metodos default en SQL
    // pero por comodidad los ponemos también acá sin que el usuario los ingrese)
    public CredencialAcceso(String hashPassword) {
        super();
        this.hashPassword = hashPassword;
        this.ultimoCambio = LocalDateTime.now();
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

    public boolean isRequireReset() {
        return requireReset;
    }

    public void setRequireReset(boolean requireReset) {
        this.requireReset = requireReset;
    }

    @Override
    public String toString() {
        return "CredencialAcceso{" + "id=" + super.getId() +", eliminado=" + super.isEliminado() +  ", hashPassword=" + hashPassword + ", salt=" + salt + ", ultimoCambio=" + ultimoCambio + ", requireReset=" + requireReset + '}';
    }

}
