/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import dao.CredencialAccesoDAO;
import dao.UsuarioDAO;
import java.util.Scanner;
import service.CredencialAccesoService;
import service.UsuarioService;

/**
 *
 * @author soilu
 */
public class AppMenu {
/**
     * Scanner único compartido por toda la aplicación.
     * IMPORTANTE: Solo debe haber UNA instancia de Scanner(System.in).
     * Múltiples instancias causan problemas de buffering de entrada.
     */
    private final Scanner scanner;

    /**
     * Handler que ejecuta las operaciones del menú.
     * Contiene toda la lógica de interacción con el usuario.
     */
    private final MenuHandler menuHandler;

    /**
     * Flag que controla el loop principal del menú.
     * Se setea a false cuando el usuario selecciona "0 - Salir".
     */
    private boolean running;

    /**
     * Constructor que inicializa la aplicación.
     *
     * Flujo de inicialización:
     * 1. Crea Scanner único para toda la aplicación
     * 2. Crea cadena de dependencias (DAOs → Services) mediante createUsuarioService()
     * 3. Crea MenuHandler con Scanner y UsuarioService
     * 4. Setea running=true para iniciar el loop
     *
     * Patrón de inyección de dependencias (DI) manual:
     * - CredencialAccesoDAO (sin dependencias)
     * - UsuarioDAO (sin dependencias)
     * - CredencialAccesoService (depende de CredencialAccesoDAO y UsuarioDAO)
     * - UsuarioService (depende de UsuarioDAO y CredencialAccesoService)
     * - MenuHandler (depende de Scanner y UsuarioService)
     *
     * Esta inicialización garantiza que todas las dependencias estén correctamente conectadas.
     */
    public AppMenu() {
        this.scanner = new Scanner(System.in);
        UsuarioService usuarioService = createUsuarioService();
        this.menuHandler = new MenuHandler(scanner, usuarioService);
        this.running = true;
    }

    /**
     * Punto de entrada de la aplicación Java.
     * Crea instancia de AppMenu y ejecuta el menú principal.
     *
     * @param args Argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }

    /**
     * Loop principal del menú.
     *
     * Flujo:
     * 1. Mientras running==true:
     *    a. Muestra menú con MenuDisplay.mostrarMenuPrincipal()
     *    b. Lee opción del usuario (scanner.nextLine())
     *    c. Convierte a int (puede lanzar NumberFormatException)
     *    d. Procesa opción con processOption()
     * 2. Si el usuario ingresa texto no numérico: Muestra mensaje de error y continúa
     * 3. Cuando running==false (opción 0): Sale del loop y cierra Scanner
     *
     * Manejo de errores:
     * - NumberFormatException: Captura entrada no numérica (ej: "abc")
     * - Muestra mensaje amigable y NO termina la aplicación
     * - El usuario puede volver a intentar
     *
     * IMPORTANTE: El Scanner se cierra al salir del loop.
     * Cerrar Scanner(System.in) cierra System.in para toda la aplicación.
     */
    public void run() {
        try (scanner) {
            while (running) {
                try {
                    MenuDisplay.mostrarMenuPrincipal();
                    int opcion = Integer.parseInt(scanner.nextLine());
                    processOption(opcion);
                } catch (NumberFormatException e) {
                    System.out.println("Entrada invalida. Por favor, ingrese un numero.");
                }
            }
        }
    }

    /**
     * Procesa la opción seleccionada por el usuario y delega a MenuHandler.
     *
     * Switch expression (Java 14+) con operador arrow (->):
     * - Más conciso que switch tradicional
     * - No requiere break (cada caso es independiente)
     * - Permite bloques con {} para múltiples statements
     *
     * Mapeo de opciones (corresponde a MenuDisplay y el switch):
     * 1  → Crear usuario
     * 2  → Listar usuarios
     * 3  → Buscar usuario
     * 4  → Actualizar usuario
     * 5  → Eliminar usuario (soft delete)
     * 6  → Recuperar usuario
     * 7  → Crear credencial de acceso
     * 8  → Listar credenciales
     * 9  → Buscar credencial por ID
     * 10 → Actualizar credencial por ID
     * 11 → Eliminar credencial por ID
     * 12 → Recuperar credencial por ID
     * 13 → Actualizar credencial por usuario
     * 0  → Salir (setea running=false para terminar el loop)
     *
     * Opción inválida: Muestra mensaje y continúa el loop.
     *
     * IMPORTANTE: Todas las excepciones de MenuHandler se capturan dentro de los métodos.
     * processOption() NO propaga excepciones al caller (run()).
     *
     * @param opcion Número de opción ingresado por el usuario
     */
    private void processOption(int opcion) {
        switch (opcion) {
            case 1 -> menuHandler.crearUsuario();
            case 2 -> menuHandler.listarUsuarios();
            case 3 -> menuHandler.buscarUsuario();
            case 4 -> menuHandler.actualizarUsuario();
            case 5 -> menuHandler.eliminarUsuario();
            case 6 -> menuHandler.recuperarUsuario();
            case 7 -> menuHandler.crearCredencialAcceso();
            case 8 -> menuHandler.listarCredenciales();
            case 9 -> menuHandler.buscarCredencialPorId();
            case 10 -> menuHandler.actualizarCredencialPorId();
            case 11 -> menuHandler.eliminarCredencialPorId();
            case 12 -> menuHandler.recuperarCredencialPorId();
            case 13 -> menuHandler.actualizarCredencialPorUsuario();
            case 0 -> {
                System.out.println("Saliendo...");
                running = false;
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    /**
     * Factory method que crea la cadena de dependencias de servicios.
     * Implementa inyección de dependencias manual.
     *
     * Orden de creación (bottom-up desde la capa más baja):
     * 1. CredencialAccesoDAO: Sin dependencias
     * 2. UsuarioDAO: Sin dependencias
     * 3. CredencialAccesoService: Depende de CredencialAccesoDAO y UsuarioDAO
     * 4. UsuarioService: Depende de UsuarioDAO y CredencialAccesoService
     *
     * Arquitectura resultante (4 capas):
     * Main (AppMenu, MenuHandler)
     *   ↓
     * Service (UsuarioService, CredencialAccesoService)
     *   ↓
     * DAO (UsuarioDAO, CredencialAccesoDAO)
     *   ↓
     * Models (Usuario, CredencialAcceso, Base)
     *
     * ¿Por qué CredencialAccesoService necesita UsuarioDAO?
     * - Para validar/asociar usuarios al gestionar credenciales.
     * - Para operaciones transaccionales coordinadas (ej. buscar usuario antes de crear credencial)
     *
     * ¿Por qué UsuarioService necesita CredencialAccesoService?
     * - Para insertar/actualizar/eliminar credenciales al crear/actualizar/eliminar usuarios.
     * - Para gestionar el ciclo de vida de la credencial junto con el usuario.
     *
     * Patrón: Factory Method para construcción de dependencias
     *
     * @return UsuarioService completamente inicializado con todas sus dependencias
     */
    private UsuarioService createUsuarioService() {
        CredencialAccesoDAO credencialDAO = new CredencialAccesoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        CredencialAccesoService credencialAccesoService = new CredencialAccesoService(credencialDAO, usuarioDAO);
        return new UsuarioService(usuarioDAO, credencialAccesoService);
    }
}
