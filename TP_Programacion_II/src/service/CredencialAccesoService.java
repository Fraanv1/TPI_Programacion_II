/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import java.sql.Connection;
import dao.CredencialAccesoDAO;
import dao.UsuarioDAO;
import java.time.LocalDateTime;
import java.util.List;
import model.CredencialAcceso;
import model.Usuario;
import utils.HashingUtil;

/**
 * **Capa de Servicio para la entidad CredencialAcceso.**
 *
 * Responsabilidades:
 * 1. **Validación de Negocio:** Aplica reglas de negocio (ej. campos no nulos).
 * 2. **Exponer Métodos Transaccionales:** Provee los métodos `...Tx` para
 * que otros servicios (como UsuarioService) puedan coordinar operaciones
 * atómicas sin que este servicio maneje la conexión.
 *
 * @author soilu
 */
public class CredencialAccesoService implements GenericService<CredencialAcceso> {

    private final CredencialAccesoDAO credencialAccesoDAO;
    

    private final UsuarioDAO usuarioDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que los DAOs no sean null (fail-fast).
     *
     * @param credencialAccesoDAO DAO de credenciales (concreto)
     * @param usuarioDAO DAO de usuarios (necesario para validaciones de borrado)
     * @throws IllegalArgumentException si credencialAccesoDAO es null
     */

    public CredencialAccesoService(CredencialAccesoDAO credencialAccesoDAO, UsuarioDAO usuarioDAO) {
        if (credencialAccesoDAO == null) {
            throw new IllegalArgumentException("CredencialAccesoDAO no puede ser null");
        }
        if (usuarioDAO == null) {
            throw new IllegalArgumentException("UsuarioDAO no puede ser null");
        }
        this.credencialAccesoDAO = credencialAccesoDAO;
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Inserta una nueva credencial (versión no transaccional).
     *
     * @param credencial Credencial a insertar
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(CredencialAcceso credencial) throws Exception {
        prepararCredencialParaInsertar(credencial);
        validateCredencial(credencial);
        
        credencialAccesoDAO.insertar(credencial);
    }

    /**
     * Actualiza una credencial (versión no transaccional).
     *
     * @param credencial Credencial con los datos actualizados
     * @throws Exception Si la validación falla o la credencial no existe
     */
    @Override
    public void actualizar(CredencialAcceso credencial) throws Exception {
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0 para actualizar");
        }
        
        prepararCredencialParaActualizar(credencial);
        validateCredencial(credencial);
        
        credencialAccesoDAO.actualizar(credencial);
    }

    /**
     * Elimina una credencial (versión no transaccional).
     *
     * @param id ID de la credencial a eliminar
     * @throws Exception Si id <= 0, no existe, o LA CREDENCIAL ESTÁ EN USO.
     */
    @Override
    public void eliminar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        
        Usuario usuarioAsociado = usuarioDAO.buscarPorCredencialIdEnCualquierEstado(id);
        if (usuarioAsociado != null && !usuarioAsociado.isEliminado()) { // Se verifica si el usuario está eliminado, la cual es la única forma de eliminar una credencial si es que está asociada a un usuario
            throw new IllegalStateException(
                "No se puede eliminar la credencial ID " + id + " porque está en uso por el usuario: " + usuarioAsociado.getUsername()
            );
        }
        
        credencialAccesoDAO.eliminar(id);
    }
    
    @Override
    public void recuperar(long id) throws Exception {
        try {
            if (id <= 0) {
                throw new IllegalArgumentException("El ID debe ser mayor a 0");
            }
            credencialAccesoDAO.recuperar(id);
         } catch (Exception e) {
             System.out.println("Credencial no encontrada");
         }
    }

    @Override
    public CredencialAcceso getById(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return credencialAccesoDAO.getById(id);
    }

    @Override
    public List<CredencialAcceso> getAll() throws Exception {
        return credencialAccesoDAO.getAll();
    }

    
    // ====================================================
    // MÉTODOS CON TRANSACCIONES (Usados por UsuarioService)
    // ====================================================


    public void insertarTx(CredencialAcceso credencial, Connection conn) throws Exception {
        prepararCredencialParaInsertar(credencial);
        validateCredencial(credencial);
        
        credencialAccesoDAO.insertarTx(credencial, conn);
    }


    public void actualizarTx(CredencialAcceso credencial, Connection conn) throws Exception {
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0 para actualizar");
        }
        
        prepararCredencialParaActualizar(credencial);
        validateCredencial(credencial);
        
        credencialAccesoDAO.actualizarTx(credencial, conn);
    }

    /**
     * Elimina (soft delete) una credencial DENTRO de una transacción existente.
     *
     * @param id ID de la credencial a eliminar
     * @param conn Conexión transaccional
     * @throws Exception Si el ID es inválido, error de BD, o LA CREDENCIAL ESTÁ EN USO.
     */

    public void eliminarTx(long id, Connection conn) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        // Asumimos ue UsuarioService sabe lo que hace, y no incluimos más validaciones (esto nos sirve para poder eliminar un usuario y su credencial correctamente)
        credencialAccesoDAO.eliminarTx(id, conn);
    }
    

    public void recuperarTx(long id, Connection conn) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialAccesoDAO.recuperarTx(id, conn);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN Y LÓGICA (Privados)
    // =========================================================================

    /**
     * Valida que una credencial tenga datos correctos y listos para la BD.
     * Debe llamarse DESPUÉS de preparar la credencial.
     *
     * @param credencial Credencial a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateCredencial(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }
        
        if (credencial.getHashPassword() == null || credencial.getHashPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("El hashPassword no puede estar vacío");
        }
        if (credencial.getHashPassword().length() > 255) {
            throw new IllegalArgumentException("El hashPassword no puede tener mas de 255 caracteres");
        }
        
        if (credencial.getSalt() == null || credencial.getSalt().trim().isEmpty()) {
            throw new IllegalArgumentException("El salt no puede estar vacío");
        }
        if (credencial.getSalt().length() > 64) {
            throw new IllegalArgumentException("El salt no puede tener mas de 64 caracteres");
        }
    }
    
    /**
     * Prepara una credencial para una operación de INSERT.
     * Siempre genera salt, hash y timestamp.
     */
    private void prepararCredencialParaInsertar(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }

        credencial.setUltimoCambio(LocalDateTime.now());

        String passwordPlano = credencial.getHashPassword(); // Asumimos que acá viene el texto plano
        if (passwordPlano == null || passwordPlano.trim().isEmpty()) {
             throw new IllegalArgumentException("El password (para hashear) no puede estar vacío");
        }
        
        String salt = HashingUtil.generarSalt();
        String hash = HashingUtil.hashPassword(passwordPlano, salt);

        credencial.setSalt(salt);
        credencial.setHashPassword(hash);
    }

    /**
     * Prepara una credencial para una operación de UPDATE.
     * Solo genera hash y salt si el salt fue seteado a null (la señal).
     * Siempre actualiza el timestamp.
     */
    private void prepararCredencialParaActualizar(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }

        credencial.setUltimoCambio(LocalDateTime.now());

        // Si el Salt es null, es la señal del MenuHandler de que
        // hashPassword contiene texto plano y necesita ser re-hasheado.
        if (credencial.getSalt() == null) {
            String passwordPlano = credencial.getHashPassword();
            if (passwordPlano == null || passwordPlano.trim().isEmpty()) {
                throw new IllegalArgumentException("Se intentó actualizar el password a vacío");
            }
            
            String nuevoSalt = HashingUtil.generarSalt();
            String nuevoHash = HashingUtil.hashPassword(passwordPlano, nuevoSalt);

            credencial.setSalt(nuevoSalt);
            credencial.setHashPassword(nuevoHash);
        }
        // Si el Salt NO es null, significa que MenuHandler no lo cambió
        // (porque no se quería actualizar el pass), y el hash/salt son los
        // originales. No hacemos nada.
    }

}