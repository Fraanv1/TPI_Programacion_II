/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import config.DatabaseConnection;
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
     * 3. **Inicia Transacción.**
     * 4. Aplica lógica de negocio (setea 'ultimoCambio' en la credencial).
     * 5. Inserta la CredencialAcceso (para obtener su ID).
     * 6. Inserta el Usuario (usando el ID de la credencial).
     * 7. **Commit** (si todo fue exitoso).
     * 8. **Rollback** (si alguna operación falló).
     *
     * @param usuario Usuario a insertar (debe incluir su objeto CredencialAcceso)
     * @throws Exception Si la validación falla o hay un error de BD.
     */
    @Override
    public void insertar(Usuario usuario) throws Exception {
        validateUsuario(usuario);
        validateUsername(usuario.getUsername(), null); 
        validateEmail(usuario.getEmail(), null); 

        Connection conn = null; 
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            CredencialAcceso cred = usuario.getCredencial();
            
            credencialAccesoService.insertarTx(cred, conn);

            usuarioDAO.insertarTx(usuario, conn);

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new Exception("Error transaccional al insertar usuario: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Actualiza un Usuario y su CredencialAcceso de forma transaccional.
     *
     * Flujo Transaccional:
     * 1. Valida los datos del usuario.
     * 2. Valida la unicidad (excepto para el propio usuario).
     * 3. **Inicia Transacción.**
     * 4. Aplica lógica (ej. si se cambió la pass, setear 'ultimoCambio').
     * 5. Actualiza la CredencialAcceso (ej. nuevo hash).
     * 6. Actualiza el Usuario (ej. nuevo email, username).
     * 7. **Commit** o **Rollback**.
     *
     * @param usuario Usuario con los datos actualizados
     * @throws Exception Si la validación falla o hay un error de BD.
     */
    @Override
    public void actualizar(Usuario usuario) throws Exception {
        validateUsuario(usuario);
        if (usuario.getId() <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser mayor a 0 para actualizar");
        }
        validateUsername(usuario.getUsername(), usuario.getId()); 
        validateEmail(usuario.getEmail(), usuario.getId()); 

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            CredencialAcceso cred = usuario.getCredencial();
            if (cred != null) {
                credencialAccesoService.actualizarTx(cred, conn); 
            }

            usuarioDAO.actualizarTx(usuario, conn); 

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new Exception("Error transaccional al actualizar usuario: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Elimina (soft delete) un Usuario y su CredencialAcceso de forma transaccional.
     *
     * Flujo Transaccional:
     * 1. **Inicia Transacción.**
     * 2. Obtiene el Usuario (para saber qué CredencialAcceso eliminar).
     * 3. Elimina (soft delete) el Usuario.
     * 4. Elimina (soft delete) la CredencialAcceso asociada.
     * 5. **Commit** o **Rollback**.
     *
     * @param id ID del usuario a eliminar
     * @throws Exception Si el ID no existe o hay un error de BD.
     */
    @Override
    public void eliminar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Usuario usuario = usuarioDAO.getById(id);
            if (usuario == null) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }

            usuarioDAO.eliminarTx(id, conn); 

            if (usuario.getCredencial() != null) {
                credencialAccesoService.eliminarTx(usuario.getCredencial().getId(), conn); 
            }

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new Exception("Error transaccional al eliminar usuario: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
    
    /**
     * Recupera un Usuario y su CredencialAcceso de forma
     * transaccional.
     *
     * Flujo Transaccional: 1. **Inicia Transacción.** 2. Obtiene el Usuario
     * (para saber qué CredencialAcceso recuperar). 3. Recupera el
     * Usuario. 4. Recupera la CredencialAcceso asociada. 5.
     * **Commit** o **Rollback**.
     *
     * @param id ID del usuario a eliminar
     * @throws Exception Si el ID no existe o hay un error de BD.
     */
@Override
    public void recuperar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Esto hace que 'eliminado' vuelva a ser FALSE
            usuarioDAO.recuperarTx(id, conn);

            // Lo buscamos para saber qué credencial debemos recuperar, antes no lo podiamos hacer ya que getById() solo busca usuarios con 'eliminado = false'
            Usuario usuario = usuarioDAO.getById(id); 
            
            if (usuario == null) {
                // Si sigue siendo null, significa que el ID nunca existió
                throw new SQLException("No se encontró usuario con ID: " + id);
            }

            // Recupera la credencial del usuario
            if (usuario.getCredencial() != null) {
                credencialAccesoService.recuperarTx(usuario.getCredencial().getId(), conn);
            }

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw new Exception("Error transaccional al recuperar usuario: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
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