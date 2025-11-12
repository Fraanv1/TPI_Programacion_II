/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

// Importar la conexión para los métodos transaccionales
import java.sql.Connection; 
import dao.CredencialAccesoDAO; // Importar el DAO concreto
import java.time.LocalDateTime;
import java.util.List;
import model.CredencialAcceso;
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

    /**
     * DAO para acceso a datos de credenciales.
     * Inyectado en el constructor (Dependency Injection).
     * * **Importante:** Se usa el DAO concreto (CredencialAccesoDAO) y no
     * el genérico (GenericDAO) para poder acceder a los métodos
     * transaccionales (ej. insertarTx, actualizarTx).
     */
    private final CredencialAccesoDAO credencialAccesoDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param credencialAccesoDAO DAO de credenciales (concreto)
     * @throws IllegalArgumentException si credencialAccesoDAO es null
     */
    public CredencialAccesoService(CredencialAccesoDAO credencialAccesoDAO) {
        if (credencialAccesoDAO == null) {
            throw new IllegalArgumentException("CredencialAccesoDAO no puede ser null");
        }
        this.credencialAccesoDAO = credencialAccesoDAO;
    }

    /**
     * Inserta una nueva credencial (versión no transaccional).
     * Este método abre y cierra su propia conexión.
     *
     * @param credencial Credencial a insertar
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(CredencialAcceso credencial) throws Exception {
        validateCredencial(credencial);
        credencial.setUltimoCambio(LocalDateTime.now());
        prepararCredencialParaInsertar(credencial);
        
        credencialAccesoDAO.insertar(credencial);
    }

    /**
     * Actualiza una credencial (versión no transaccional).
     * Este método abre y cierra su propia conexión.
     *
     * @param credencial Credencial con los datos actualizados
     * @throws Exception Si la validación falla o la credencial no existe
     */
    @Override
    public void actualizar(CredencialAcceso credencial) throws Exception {
        validateCredencial(credencial);
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0 para actualizar");
        }
        credencial.setUltimoCambio(LocalDateTime.now());
        prepararCredencialParaActualizar(credencial);
        credencialAccesoDAO.actualizar(credencial);
    }

    /**
     * Elimina una credencial (versión no transaccional).
     * Este método abre y cierra su propia conexión.
     *
     * @param id ID de la credencial a eliminar
     * @throws Exception Si id <= 0 o no existe la credencial
     */
    @Override
    public void eliminar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialAccesoDAO.eliminar(id);
    }
    
    /**
     * Recupera una credencial (versión no transaccional). Este método abre y
     * cierra su propia conexión.
     *
     * @param id ID de la credencial a recuperar
     * @throws Exception Si id <= 0 o no existe la credencial
     */
    @Override
    public void recuperar(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialAccesoDAO.recuperar(id);
    }

    /**
     * Obtiene una credencial por su ID.
     * (Método de solo lectura, no requiere transacción manual).
     *
     * @param id ID de la credencial a buscar
     * @return Credencial encontrada, o null si no existe
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public CredencialAcceso getById(long id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return credencialAccesoDAO.getById(id);
    }

    /**
     * Obtiene todas las credenciales activas.
     * (Método de solo lectura, no requiere transacción manual).
     *
     * @return Lista de credenciales activas
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<CredencialAcceso> getAll() throws Exception {
        return credencialAccesoDAO.getAll();
    }

    
    
    // ====================================================
    // MÉTODOS CON TRANSACCIONES  (Usados por UsuarioService)
    // ====================================================

    /**
     * Inserta una credencial DENTRO de una transacción existente.
     * No abre ni cierra la conexión. Delega en el método Tx del DAO.
     *
     * @param credencial Credencial a insertar
     * @param conn Conexión transaccional
     * @throws Exception Si la validación falla o error de BD
     */
    public void insertarTx(CredencialAcceso credencial, Connection conn) throws Exception {
        validateCredencial(credencial);
        credencial.setUltimoCambio(LocalDateTime.now());
        prepararCredencialParaInsertar(credencial);
        
        credencialAccesoDAO.insertarTx(credencial, conn);
    }

    /**
     * Actualiza una credencial DENTRO de una transacción existente.
     * No abre ni cierra la conexión. Delega en el método Tx del DAO.
     *
     * @param credencial Credencial a actualizar
     * @param conn Conexión transaccional
     * @throws Exception Si la validación falla o error de BD
     */
    public void actualizarTx(CredencialAcceso credencial, Connection conn) throws Exception {
        validateCredencial(credencial);
        prepararCredencialParaActualizar(credencial);
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0 para actualizar");
        }
        credencial.setUltimoCambio(LocalDateTime.now()); 
        credencialAccesoDAO.actualizarTx(credencial, conn);
    }

    /**
     * Elimina (soft delete) una credencial DENTRO de una transacción existente.
     * No abre ni cierra la conexión. Delega en el método Tx del DAO.
     *
     * @param id ID de la credencial a eliminar
     * @param conn Conexión transaccional
     * @throws Exception Si el ID es inválido o error de BD
     */
    public void eliminarTx(long id, Connection conn) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialAccesoDAO.eliminarTx(id, conn);
    }
    
    /**
     * Recupera una credencial DENTRO de una transacción existente.
     * No abre ni cierra la conexión. Delega en el método Tx del DAO.
     *
     * @param id ID de la credencial a recuperar
     * @param conn Conexión transaccional
     * @throws Exception Si el ID es inválido o error de BD
     */
    public void recuperarTx(long id, Connection conn) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialAccesoDAO.recuperarTx(id, conn);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN (Lógica de Negocio)
    // =========================================================================

    /**
     * Valida que una credencial tenga datos correctos.
     *
     * @param credencial Credencial a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateCredencial(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0");
        }
        if (credencial.getHashPassword() == null || credencial.getHashPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("El hashPassword no puede estar vacío");
        }
        if (credencial.getHashPassword().length() > 255) {
            throw new IllegalArgumentException("El hashPassword no puede tener mas de 255 caracteres");
        }
        if (credencial.getSalt().length() > 64) {
            throw new IllegalArgumentException("El salt no puede tener mas de 64 caracteres");
        }
    }
    
    /**
     * Prepara una credencial para una operación de INSERT. Siempre genera salt y hash.
     */
    private void prepararCredencialParaInsertar(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }

        credencial.setUltimoCambio(LocalDateTime.now());

        String passwordPlano = credencial.getHashPassword(); // Asumimos que acá viene el texto plano
        String salt = HashingUtil.generarSalt();
        String hash = HashingUtil.hashPassword(passwordPlano, salt);

        credencial.setSalt(salt);
        credencial.setHashPassword(hash);
    }

    /**
     * Prepara una credencial para una operación de UPDATE. Solo genera hash y
     * salt si el salt fue seteado a null (la señal del MenuHandler).
     */
    private void prepararCredencialParaActualizar(CredencialAcceso credencial) {
        if (credencial == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }

        credencial.setUltimoCambio(LocalDateTime.now());

        // Si el Salt es null, es la señal del MenuHandler de que
        // hashPassword contiene texto plano y necesita ser hasheado.
        if (credencial.getSalt() == null) {
            String passwordPlano = credencial.getHashPassword();
            String nuevoSalt = HashingUtil.generarSalt();
            String nuevoHash = HashingUtil.hashPassword(passwordPlano, nuevoSalt);

            credencial.setSalt(nuevoSalt);
            credencial.setHashPassword(nuevoHash);
        }
        // Si el Salt NO es null, significa que MenuHandler no lo cambió,
        // y el hash/salt son los originales. Por lo que no hacemos nada.
    }

}