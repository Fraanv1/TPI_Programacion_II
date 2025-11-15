/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;


/**
 * Punto de entrada alternativo de la aplicación.
 * Clase simple que delega inmediatamente a AppMenu.
/**
 *
 * @author soilu
 */
public class Main {
    /**
     * Punto de entrada alternativo de la aplicación Java.
     * Crea AppMenu y ejecuta el menú principal.
     *
     * Flujo:
     * 1. Crea instancia de AppMenu (inicializa toda la aplicación)
     * 2. Llama a app.run() que ejecuta el loop del menú
     * 3. Cuando el usuario sale (opción 0), run() termina y la aplicación finaliza
     *
     * @param args Argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }
}
