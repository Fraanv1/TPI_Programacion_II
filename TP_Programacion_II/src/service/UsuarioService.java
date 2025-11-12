/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import config.DatabaseConnection;
import config.TransactionManager; // Importamos el TransactionManager
import dao.UsuarioDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.CredencialAcceso;
import model.Usuario;

/**
 * **Capa de Servicio para la entidad Usuario.**
 *
 * Responsabilidades:
 * 1. **Orquestación de Transacciones:** Garantiza que las operaciones complejas
 * (ej. crear Usuario + Credencial) se completen atómicamente (o todo o nada).
 * 2. **Lógica de Negocio:** Aplica reglas de negocio (ej. validaciones,
 * setear timestamps).
 * 3. **Coordinación de DAOs/Servicios:** Actúa como intermediario entre los
 * controladores (o vistas) y las capas de acceso a datos.
 *
 * @author soilu
 */
public class UsuarioService implements GenericService<Usuario> {

    /**
     * DAO para acceso a datos de Usuarios.
     * Inyectado en el constructor (Dependency Injection).
     */
    private final UsuarioDAO usuarioDAO;

    /**
     * Servicio de CredencialAcceso para coordinar operaciones transaccionales.
     * El UsuarioService necesita este servicio para gestionar la entidad
     * CredencialAcceso como parte de la misma transacción del Usuario.
     */
    private final CredencialAccesoService credencialAccesoService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que ambas dependencias no sean null (fail-fast).
     *
     * @param usuarioDAO DAO de Usuarios
     * @param credencialAccesoService Servicio de CredencialAcceso
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public UsuarioService(UsuarioDAO usuarioDAO, CredencialAccesoService credencialAccesoService) {
        if (usuarioDAO == null) {
            throw new IllegalArgumentException("UsuarioDAO no puede ser null");
        }
        if (credencialAccesoService == null) {
            throw new IllegalArgumentException("CredencialAccesoService no puede ser null");
        }
        this.usuarioDAO = usuarioDAO;
        this.credencialAccesoService = credencialAccesoService;
    }

    /**
     * Inserta un nuevo Usuario y su CredencialAcceso de forma transaccional.
     *
     * Flujo Transaccional (Todo o Nada):
     * 1. Valida los datos del usuario (username, email, credencial no nula).
     * 2. Valida la unicidad de username y email (RN).
     * 3. **Inicia Transacción (con TransactionManager).**
     * 4. Inserta la CredencialAcceso (obtiene ID).
     * 5. Inserta el Usuario (usa el ID de la credencial).
     * 6. **Commit** (si todo fue exitoso).
     * 7. **Rollback** (automático por TransactionManager si falla).
     *
     * @param usuario Usuario a insertar (debe incluir su objeto CredencialAcceso)
     * @throws Exception Si la validación falla o hay un error de BD.
     */
    @Override
    public void insertar(Usuario usuario) throws Exception {
        validateUsuario(usuario);
        validateUsername(usuario.getUsername(), null); 
        validateEmail(usuario.getEmail(), null); 

        // El try-with-resources maneja el TransactionManager (y la conexión)
        // El .close() del TransactionManager hará rollback si no se hizo commit.
        try (Connection conn = DatabaseConnection.getConnection();
             TransactionManager tx = new TransactionManager(conn)) {

            tx.startTransaction();

            CredencialAcceso cred = usuario.getCredencial();
            
            credencialAccesoService.insertarTx(cred, conn);
            usuarioDAO.insertarTx(usuario, conn);

            tx.commit();

        } catch (Exception e) {
            // No necesitamos rollback manual, tx.close() lo maneja.
            throw new Exception("Error transaccional al insertar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza un Usuario y su CredencialAcceso de forma transaccional.
     *
     * Flujo Transaccional:
     * 1. Valida los datos del usuario.
     * 2. Valida la unicidad (excepto para el propio usuario).
     * 3. **Inicia Transacción (con TransactionManager).**
     * 4. Actualiza la CredencialAcceso (si es necesario).
     * 5. Actualiza el Usuario.
     * 6. **Commit** o **Rollback** (automático).
     *
     * @param usuario Usuario con los datos actualizados
     * @throws Exception Si la validación falla o hay un error de BD.
     */
    @Override
    public void actualizar(Usuario usuario) throws Exception {
        if (usuario.getId() <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser mayor a 0 para actualizar");
        }
        validateUsuario(usuario);
        validateUsername(usuario.getUsername(), usuario.getId()); 
        validateEmail(usuario.getEmail(), usuario.getId()); 

        try (Connection conn = DatabaseConnection.getConnection();
             TransactionManager tx = new TransactionManager(conn)) {
            
            tx.startTransaction();

            CredencialAcceso cred = usuario.getCredencial();
            if (cred != null) {
                credencialAccesoService.actualizarTx(cred, conn); 
            }

            usuarioDAO.actualizarTx(usuario, conn); 

            tx.commit();

        } catch (Exception e) {
            throw new Exception("Error transaccional al actualizar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina (soft delete) un Usuario y su CredencialAcceso de forma transaccional.
     *
     * Flujo Transaccional:
     * 1. **Inicia Transacción (con TransactionManager).**
     * 2. Obtiene el Usuario (para saber qué CredencialAcceso eliminar).
     * 3. Elimina (soft delete) el Usuario.
     * 4. Elimina (soft delete) la CredencialAcceso asociada.
     * 5. **Commit** o **Rollback** (automático).
     *
     * @param id ID del usuario a eliminar
     * @throws Exception Si el ID no existe o hay un error de BD.
     */
    @Override
    public void eliminar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             TransactionManager tx = new TransactionManager(conn)) {
            
            tx.startTransaction();

            Usuario usuario = usuarioDAO.getById(id);
            if (usuario == null) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }

            usuarioDAO.eliminarTx(id, conn); 

            if (usuario.getCredencial() != null) {
                credencialAccesoService.eliminarTx(usuario.getCredencial().getId(), conn); 
            }

            tx.commit();

        } catch (Exception e) {
            throw new Exception("Error transaccional al eliminar usuario: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recupera un Usuario y su CredencialAcceso de forma
     * transaccional.
     *
     * Flujo Transaccional: 1. **Inicia Transacción (con TransactionManager).**
     * 2. Recupera el Usuario.
     * 3. Obtiene el Usuario (para saber qué CredencialAcceso recuperar).
     * 4. Recupera la CredencialAcceso asociada.
     * 5. **Commit** o **Rollback** (automático).
     *
     * @param id ID del usuario a recuperar
     * @throws Exception Si el ID no existe o hay un error de BD.
     */
@Override
    public void recuperar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             TransactionManager tx = new TransactionManager(conn)) {
            
            tx.startTransaction();

            usuarioDAO.recuperarTx(id, conn);

            Usuario usuario = usuarioDAO.getById(id); 
            
            if (usuario == null) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }

            if (usuario.getCredencial() != null) {
                credencialAccesoService.recuperarTx(usuario.getCredencial().getId(), conn);
            }

            tx.commit();

        } catch (Exception e) {
            throw new Exception("Error transaccional al recuperar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public Usuario getById(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return usuarioDAO.getById(id);
    }

    @Override
    public List<Usuario> getAll() throws Exception {
        return usuarioDAO.getAll();
    }

    public CredencialAccesoService getCredencialAccesoService() {
        return this.credencialAccesoService;
    }

    public Usuario buscarPorUsername(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        return usuarioDAO.buscarPorUsername(username);
    }

    public Usuario buscarPorEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        return usuarioDAO.buscarPorEmail(email);
    }

    
    /**
     * Valida que un usuario tenga los campos obligatorios.
     * @param usuario Usuario a validar
     * @throws IllegalArgumentException Si algún campo es inválido
     */
    private void validateUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }
        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        if (usuario.getCredencial() == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }
    }

    /**
     * Valida la unicidad del Email, y si tiene menos de 120 caracteres.
     *
     * @param email Email a validar
     * @param usuarioId ID del usuario (null para INSERT, ID para UPDATE)
     * @throws Exception Si el email ya pertenece a OTRO usuario.
     */
    private void validateEmail(String email, Long usuarioId) throws Exception {
        Usuario existente = usuarioDAO.buscarPorEmail(email);

        if (existente != null && (usuarioId == null || !existente.getId().equals(usuarioId))) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + email);
        }
        if (email.length() > 120) {
            throw new IllegalArgumentException("El email no puede tener mas de 120 caracteres");
        }
    }

    /**
     * Valida la unicidad del Username y su longitud.
     *
     * @param username Username a validar
     * @param usuarioId ID del usuario (null para INSERT, ID para UPDATE)
     * @throws Exception Si el username ya pertenece a OTRO usuario O tiene mas de 30 caracteres.
     */
    private void validateUsername(String username, Long usuarioId) throws Exception {
        Usuario existente = usuarioDAO.buscarPorUsername(username);
        
        if (existente != null && (usuarioId == null || !existente.getId().equals(usuarioId))) {
            throw new IllegalArgumentException("Ya existe un usuario con el username: " + username);
        }
        
        if (username.length() > 30) {
            throw new IllegalArgumentException("El username no puede tener mas de 30 caracteres");
        }
    }
}