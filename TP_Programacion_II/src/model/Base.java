/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author soilu
 */
public class Base {
    private Long id;
    private boolean eliminado = false;  // Valor por defecto, para no tener que repetirlo en todos los modelos

    // Constructor completo
    public Base(Long id, boolean eliminado) {
        this.id = id;
        this.eliminado = eliminado;
    }

    // Constructor vacio
    public Base() {
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}
