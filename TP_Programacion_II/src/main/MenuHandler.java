/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import java.util.List;
import java.util.Scanner;
import model.CredencialAcceso;
import model.Usuario;
import service.CredencialAccesoService;
import service.UsuarioService;

/**
 * **Controlador de las operaciones del menú (Menu Handler).**
 * Gestiona toda la lógica de interacción con el usuario (la "Vista").
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner).
 * - Invocar los servicios de negocio (UsuarioService, CredencialAccesoService).
 * - Mostrar resultados y mensajes de error al usuario.
 * - Coordinar la recolección de datos para operaciones complejas.
 *
 * Arquitectura:
 * MenuHandler (Vista) → Service (Lógica de Negocio/Transacciones) → DAO (Acceso a BD)
 *
 * IMPORTANTE: Este handler NO contiene lógica de negocio (como hashing o
 * transacciones).
 * Solo recolecta datos crudos (Strings), los asigna a los objetos Modelo,
 * y los pasa a la capa de Servicio para su procesamiento.
 *
 * @author soilu
 */
public class MenuHandler {

    /**
     * Scanner compartido para leer entrada del usuario.
     * Inyectado desde la clase principal (ej. AppMenu).
     */
    private final Scanner scanner;

    /**
     * Servicio de usuarios para operaciones CRUD coordinadas.
     * Proporciona acceso al CredencialAccesoService.
     */
    private final UsuarioService usuarioService;
    
    /**
     * Servicio de credenciales, obtenido del servicio de usuario.
     * Se usa para operaciones que solo afectan a las credenciales.
     */
    private final CredencialAccesoService credencialAccesoService;


    /**
     * Constructor con inyección de dependencias.
     * Valida que las dependencias no sean null (fail-fast).
     *
     * @param scanner Scanner compartido para entrada de usuario
     * @param usuarioService Servicio de usuarios
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, UsuarioService usuarioService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (usuarioService == null) {
            throw new IllegalArgumentException("UsuarioService no puede ser null");
        }
        this.scanner = scanner;
        this.usuarioService = usuarioService;
        this.credencialAccesoService = usuarioService.getCredencialAccesoService();
    }

    /**
     * Opción 1: Crear nuevo Usuario (con Credencial).
     *
     * Flujo:
     * 1. Solicita username, email.
     * 2. Llama al método auxiliar crearCredencial() para pedir el password.
     * 3. Crea el objeto Usuario y le asocia la Credencial.
     * 4. Invoca usuarioService.insertar(usuario) que:
     * - Inicia la transacción.
     * - Llama a credencialAccesoService.insertarTx() (que hashea el pass y guarda).
     * - Llama a usuarioDAO.insertarTx() (que guarda el usuario).
     * - Hace Commit o Rollback.
     */
    public void crearUsuario() {
        try {
            System.out.println("--- Crear Nuevo Usuario ---");
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            
            System.out.println("...Configurando Credencial...");
            CredencialAcceso credencial = crearCredencial(); // Pide el password

            Usuario usuario = new Usuario(username, email, credencial);
            
            usuarioService.insertar(usuario);
            
            System.out.println("Usuario creado exitosamente con ID: " + usuario.getId());
        } catch (Exception e) {
            System.err.println("Error al crear el usuario: " + e.getMessage());
        }
    }

/**
     * Opción 2: Listar todos los usuarios.
     *
     * Flujo:
     * 1. Llama a usuarioService.getAll().
     * 2. Itera sobre la lista y llama a imprimirUsuario() para cada uno.
     */
    public void listarUsuarios() {
        try {
            List<Usuario> usuarios = usuarioService.getAll();
            if (usuarios.isEmpty()) {
                System.out.println("No se encontraron usuarios.");
                return;
            }
            
            System.out.println("--- Listado de Todos los Usuarios ---");
            for (Usuario u : usuarios) {
                imprimirUsuario(u); // Usa el método auxiliar
            }
        } catch (Exception e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Buscar un Usuario específico.
     *
     * Submenú:
     * 1. Buscar por Username (búsqueda exacta).
     * 2. Buscar por Email (búsqueda exacta).
     *
     * Flujo:
     * 1. Pide la opción (1 o 2).
     * 2. Pide el término de búsqueda (username o email).
     * 3. Llama al servicio correspondiente (buscarPorUsername o buscarPorEmail).
     * 4. Llama a imprimirUsuario() con el resultado.
     */
    public void buscarUsuario() {
        try {
            System.out.print("¿Buscar por (1) Username o (2) Email? Ingrese opcion: ");
            int opcion = Integer.parseInt(scanner.nextLine());

            Usuario usuario;

            switch (opcion) {
                case 1 ->  {
                    System.out.print("Ingrese username a buscar: ");
                    String username = scanner.nextLine().trim();
                    usuario = usuarioService.buscarPorUsername(username);
                }
                case 2 ->  {
                    System.out.print("Ingrese email a buscar: ");
                    String email = scanner.nextLine().trim();
                    usuario = usuarioService.buscarPorEmail(email);
                }
                default -> {
                    System.out.println("Opcion invalida.");
                    return; // Salir del método
                }
            }

            imprimirUsuario(usuario); // El método auxiliar maneja si es null

        } catch (NumberFormatException e) {
            System.err.println("Error: Debe ingresar un número (1 o 2).");
        } catch (Exception e) {
            System.err.println("Error al buscar usuario: " + e.getMessage());
        }
    }

    /**
     * Opción 4: Actualizar Usuario existente.
     *
     * Flujo: 1. Solicita el criterio de búsqueda (ID, Username, Email) y busca
     * al usuario. 2. Si se encuentra, muestra valores actuales y permite
     * actualizar: - Username (Enter para mantener actual). - Email (Enter para
     * mantener actual). - Estado 'activo' (Enter para mantener actual). 3.
     * Llama a 'actualizarCredencial()' para manejar cambios en password. 4.
     * Invoca 'usuarioService.actualizar(usuario)' (método transaccional) solo
     * si se detectó algún cambio.
     *
     * Patrón "Enter para mantener": - Lee input con scanner.nextLine().trim().
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual). - Si
     * tiene valor → Actualiza el campo.
     */
    public void actualizarUsuario() {
        try {
            System.out.println("--- Actualizar Usuario ---");
            System.out.print("¿Buscar usuario a actualizar por (1) ID, (2) Username o (3) Email? Ingrese opcion: ");
            int opcionBusqueda = Integer.parseInt(scanner.nextLine());

            Usuario u; // Declaramos 'u' fuera del switch

            switch (opcionBusqueda) {
                case 1 -> {
                    System.out.print("Ingrese el ID: ");
                    long id = Long.parseLong(scanner.nextLine());
                    u = usuarioService.getById(id);
                }
                case 2 -> {
                    System.out.print("Ingrese el Username: ");
                    String usernameBusqueda = scanner.nextLine().trim();
                    u = usuarioService.buscarPorUsername(usernameBusqueda);
                }
                case 3 -> {
                    System.out.print("Ingrese el Email: ");
                    String emailBusqueda = scanner.nextLine().trim();
                    u = usuarioService.buscarPorEmail(emailBusqueda);
                }
                default -> {
                    System.out.println("Opción de búsqueda inválida.");
                    return; // Salir
                }
            }

            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }

            boolean actualizar = false;

            System.out.println("--- Actualizando Usuario: " + u.getUsername() + " ---");
            System.out.println("(Deje en blanco y aprete enter para mantener el valor actual)");

            System.out.print("Nuevo username (actual: " + u.getUsername() + "): ");

            String username = scanner.nextLine().trim();
            if (!username.isEmpty()) {
                u.setUsername(username);
                actualizar = true;
            }

            System.out.print("Nuevo email (actual: " + u.getEmail() + "): ");
            String email = scanner.nextLine().trim();
            if (!email.isEmpty()) {
                u.setEmail(email);
                actualizar = true;
            }

            System.out.print("¿Usuario activo? (s/n) (actual: " + u.isActivo() + "): ");
            String activoStr = scanner.nextLine().trim();

            if (!activoStr.isEmpty()) {
                if (activoStr.equalsIgnoreCase("s")) {
                    u.setActivo(true);
                    actualizar = true;
                } else if (activoStr.equalsIgnoreCase("n")) {
                    u.setActivo(false);
                    actualizar = true;
                } else {
                    // Si el usuario escribe algo invalido no se cambia el estado
                    System.out.println("(Entrada '" + activoStr + "' no reconocida. El estado 'activo' no se modificó.)");
                }
            }

            // Llama al método auxiliar para manejar la lógica de la credencial
            boolean actualizarCredencial = actualizarCredencial(u.getCredencial());

            if (actualizar || actualizarCredencial) {
                // Llama al service transaccional
                usuarioService.actualizar(u);
                System.out.println("Usuario actualizado exitosamente.");
            } else {
                System.out.println("No han habido cambios.");
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número valido.");
        } catch (Exception e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
        }
    }

    /**
     * Opción 5: Eliminar Usuario (soft delete transaccional).
     *
     * Flujo:
     * 1. Solicita ID del usuario.
     * 2. Pide confirmación.
     * 3. Invoca usuarioService.eliminar(id) que:
     * - Inicia la transacción.
     * - Marca 'eliminado = true' en el usuario.
     * - Marca 'eliminado = true' en la credencial asociada.
     * - Hace Commit o Rollback.
     *
     * Esta es la operación de eliminación SEGURA Y RECOMENDADA.
     */
    public void eliminarUsuario() {
        try {
            System.out.print("ID del usuario a eliminar lógicamente (esto también eliminará lógicamente su credencial): ");
            long id = Long.parseLong(scanner.nextLine());
            
            Usuario u = usuarioService.getById(id);
            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }
            
            System.out.print("¿Seguro que desea eliminar a " + u.getUsername() + "? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) { // Solo si ingresa "s" se eliminará, cualquier otro input llevará al else. 
                usuarioService.eliminar(id);
                System.out.println("Usuario y credencial asociados eliminados (soft delete) exitosamente.");
            } else {
                System.out.println("Operación cancelada.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número valido.");
        } catch (Exception e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Crear Credencial de Acceso (sin asociar a usuario).
     *
     * Flujo:
     * 1. Llama al método privado crearCredencial() para capturar el password.
     * 2. Invoca credencialAccesoService.insertar() (el método simple, no transaccional, debido a que no hace falta).
     * - El servicio se encarga de hashear y poner el timestamp.
     *
     * Uso Típico:
     * - Crear una credencial que (aún) no se asociará a un usuario.
     */
    public void crearCredencialAcceso() {
        try {
            System.out.println("--- Crear Credencial ---");
            CredencialAcceso credencial = crearCredencial();
            
            credencialAccesoService.insertar(credencial);
            
            System.out.println("Credencial creada exitosamente con ID: " + credencial.getId());
        } catch (Exception e) {
            System.err.println("Error al crear credencial: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Listar todas las Credenciales activas.
     *
     * Muestra: ID, Último Cambio, Requiere Reset, si está eliminada,
     * el hash y el salt.
     */
    public void listarCredenciales() {
        try {
            List<CredencialAcceso> credenciales = credencialAccesoService.getAll();
            if (credenciales.isEmpty()) {
                System.out.println("No se encontraron credenciales.");
                return;
            }
            System.out.println("--- Listado de Todas las Credenciales ---");
            for (CredencialAcceso c : credenciales) {
                imprimirCredencial(c);
            }
            System.out.println("---------------------------------");
        } catch (Exception e) {
            System.err.println("Error al listar credenciales: " + e.getMessage());
        }
    }

    
    /**
     * Opción 8: Buscar una Credencial de Acceso por su ID.
     *
     * Flujo: 1. Solicita el ID de la credencial. 2. Llama a
     * credencialAccesoService.getById(). 3. Llama a imprimirCredencial() con el
     * resultado.
     */
    public void buscarCredencialPorId() {
        try {
            System.out.print("Ingrese el ID de la credencial a buscar: ");
            long id = Long.parseLong(scanner.nextLine());

            CredencialAcceso credencial = credencialAccesoService.getById(id);

            imprimirCredencial(credencial); // El método maneja si es null

        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número valido.");
        } catch (Exception e) {
            System.err.println("Error al buscar la credencial: " + e.getMessage());
        }
    }
    
/**
     * Opción 9: Actualizar Credencial por su ID.
     *
     * Flujo:
     * 1. Solicita ID de la credencial y la obtiene.
     * 2. Llama al método auxiliar 'actualizarCredencial' para manejar la lógica de inputs.
     * 3. Si el auxiliar reporta cambios, invoca credencialAccesoService.actualizar().
     */
    public void actualizarCredencialPorId() {
        try {
            System.out.print("ID de la credencial a actualizar: ");
            long id = Long.parseLong(scanner.nextLine());
            CredencialAcceso cred = credencialAccesoService.getById(id);
            
            if (cred == null) {
                System.out.println("Credencial no encontrada.");
                return;
            }

            System.out.println("--- Actualizando Credencial ID: " + cred.getId() + " ---");
            System.out.println("(Deje en blanco y presione Enter para mantener el valor actual)");

            // Este método hace las preguntas y modifica el objeto 'cred'.
            boolean credencialCambiada = actualizarCredencial(cred);

            // Comprobamos si hubo cambios.
            if (credencialCambiada) {
                credencialAccesoService.actualizar(cred);
                System.out.println("Credencial actualizada exitosamente.");
            } else {
                System.out.println("No han habido cambios.");
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número.");
        } catch (Exception e) {
            System.err.println("Error al actualizar credencial: " + e.getMessage());
        }
    }


    
    /**
     * Opción 10: Eliminar Credencial por ID (PELIGROSO - soft delete directo).
     *
     * ⚠️ ADVERTENCIA: Esta operación es un soft delete de la credencial.
     * Si un Usuario está usando esta credencial, quedará en un estado
     * inconsistente (apuntando a una credencial eliminada).
     *
     * La única eliminación SEGURA es la Opción 4 (Eliminar Usuario).
     */
    public void eliminarCredencialPorId() {
        try {
            System.out.println("--- ADVERTENCIA ---");
            System.out.println("Esta operación solo elimina la credencial, no el usuario.");
            System.out.println("Si un usuario está asociado, la base de datos quedará INCONSISTENTE.");
            System.out.println("La forma SEGURA de eliminar es la Opción 4 (Eliminar Usuario).");
            System.out.print("ID de la credencial a eliminar: ");
            long id = Long.parseLong(scanner.nextLine());

            System.out.print("¿Seguro que desea continuar con esta operación peligrosa? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) { // Solo lo borra si el input es "s", cualquier otro cancela el proceso (hecho intencionalmente que no sea solo "n" para cancelar).
                credencialAccesoService.eliminar(id);
                System.out.println("Credencial eliminada.");
            } else {
                System.out.println("Operación cancelada.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número.");
        } catch (Exception e) {
            System.err.println("Error al eliminar credencial: " + e.getMessage());
        }
    }

/**
     * Opción 11: Actualizar Credencial por ID de Usuario.
     *
     * Flujo:
     * 1. Solicita ID del usuario -> getById(id).
     * 2. Obtiene la credencial del usuario.
     * 3. Llama al método auxiliar 'actualizarCredencial' para manejar la lógica de inputs.
     * 4. Si el auxiliar reporta cambios, invoca credencialAccesoService.actualizar().
     */
    public void actualizarCredencialPorUsuario() {
        try {
            System.out.print("ID del usuario cuya credencial desea actualizar: ");
            long usuarioId = Long.parseLong(scanner.nextLine());
            Usuario u = usuarioService.getById(usuarioId);

            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }

            CredencialAcceso cred = u.getCredencial();
            if (cred == null) {
                System.out.println("Este usuario no tiene una credencial (Estado inconsistente).");
                return;
            }

            System.out.println("--- Actualizando Credencial para Usuario: " + u.getUsername() + " ---");
            System.out.println("(Deje en blanco y presione Enter para mantener el valor actual)");

            
            // Llama al metodo auxiliar.
            boolean credencialCambiada = actualizarCredencial(cred);

            // Comrpueba si hubo cambios o no.
            if (credencialCambiada) {
                credencialAccesoService.actualizar(cred);
                System.out.println("Credencial actualizada exitosamente.");
            } else {
                System.out.println("No han habido cambios.");
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: El ID debe ser un número.");
        } catch (Exception e) {
            System.err.println("Error al actualizar credencial: " + e.getMessage());
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES (PRIVADOS)
    // =========================================================================

    /**
     * Método auxiliar privado: Crea un objeto CredencialAcceso pidiendo el password.
     *
     * Flujo:
     * 1. Solicita password (con trim).
     * 2. Crea objeto CredencialAcceso.
     * 3. Asigna el password en *texto plano* al campo hashPassword.
     *
     * Nota: NO persiste en BD, solo crea el objeto en memoria.
     * El CredencialAccesoService se encargará de hashear el password.
     *
     * @return CredencialAcceso nueva (no persistida, ID=0, con pass en texto plano)
     */
    private CredencialAcceso crearCredencial() {
        System.out.print("Password: ");
        String passwordPlano = scanner.nextLine().trim();
        
        CredencialAcceso cred = new CredencialAcceso();
        cred.setHashPassword(passwordPlano); // El Servicio lo hasheará
        cred.setRequireReset(false); // Valor por defecto
        // El Salt será generado por el Servicio
        
        return cred;
    }

    /**
     * Método auxiliar privado: Maneja la actualización de la credencial DENTRO
     * de la lógica de 'actualizarUsuario' (Opción 3).
     *
     * Modifica el objeto Usuario en memoria. El 'usuarioService.actualizar(u)'
     * se encargará de persistir los cambios de forma transaccional.
     *
     * @param u El objeto Usuario que se está actualizando.
     */
    private boolean actualizarCredencial(CredencialAcceso cred) {
        if (cred == null) {
            System.out.println("La credencial es nula.");
            return false;
        }

        boolean actualizar = false;
        
        System.out.print("¿Desea actualizar la password? (s/n): ");
        if (scanner.nextLine().equalsIgnoreCase("s")) {
            System.out.print("Nuevo password: ");
            String passwordPlano = scanner.nextLine().trim();

            if (!passwordPlano.isEmpty()) {
                // Seteamos el password plano. El CredencialAccesoService lo hasheará
                cred.setHashPassword(passwordPlano);
                // Ponemos el salt en null. Esta será la "señal" para que el
                // CredencialAccesoService sepa que debe volver a generar el hash y el salt.
                cred.setSalt(null);
                actualizar = true;
            }
            // Si el password está vacío (solo dio Enter), no hacemos nada
            // El objeto 'cred' mantiene su hash y salt originales
        }
        // Si la respuesta fue "n", no hacemos nada
        // El objeto 'cred' mantiene su hash y salt originales

        System.out.print("¿Requiere reseteo? (s/n) (actual: " + cred.getRequireReset() + "): ");
        String resetStr = scanner.nextLine().trim();

        if (!resetStr.isEmpty()) {
            if (resetStr.equalsIgnoreCase("s")) {
                cred.setRequireReset(true);
                actualizar = true;
            } else if (resetStr.equalsIgnoreCase("n")) {
                cred.setRequireReset(false);
                actualizar = true;
            } else {
                System.out.println("(Entrada '" + resetStr + "' no reconocida. El estado 'Requiere Reseteo' no se modificó.)");
            }
            
        }
        // Si resetStr o passwordPlano están vacíos, no se hace nada y se mantiene el valor original.
        // Devuelve true si se hicieron cambios, de lo contrario devuelve false
        return actualizar;
        
    }
    
    /**
     * Método auxiliar privado para imprimir los *campos* de una Credencial. No
     * imprime separadores ni maneja valores null. Es llamado por
     * imprimirUsuario() y listarCredenciales().
     *
     * @param c La CredencialAcceso (válida) a imprimir.
     */
    private void imprimirCredencial(CredencialAcceso c) {
        if (c == null) {
            System.out.println("No se encontró ninguna credencial que coincida.");
            return;
        }
        System.out.println("  --- Credencial (ID: " + c.getId() + ") ---");
        System.out.println("  Hash: " + c.getHashPassword());
        System.out.println("  Salt: " + c.getSalt());
        System.out.println("  Último Cambio: " + c.getUltimoCambio());
        System.out.println("  Requiere Reset: " + c.getRequireReset());
        System.out.println("  Eliminada: " + c.isEliminado());

    }
    
    /**
     * Método auxiliar privado para imprimir los detalles de un Usuario de forma
     * estandarizada.
     *
     * @param u El Usuario a imprimir.
     */
    private void imprimirUsuario(Usuario u) {
        if (u == null) {
            System.out.println("No se encontró ningún usuario que coincida.");
            return;
        }
        System.out.println("---------------------------------");
        System.out.println("ID: " + u.getId() + " | Username: " + u.getUsername());
        System.out.println("  Email: " + u.getEmail());
        System.out.println("  Fecha de Registro: " + u.getFechaRegistro());
        System.out.println("  Activo: " + u.isActivo() + ", Eliminado: " + u.isEliminado());

        if (u.getCredencial() != null) {
            imprimirCredencial(u.getCredencial());
        } else {
            System.out.println("  (Sin credencial asociada)");
        }
        System.out.println("---------------------------------");
    }
    
}