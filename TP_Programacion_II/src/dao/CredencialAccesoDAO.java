/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import config.DatabaseConnection;
import model.CredencialAcceso;

/**
 *
 * @author soilu
 */


/**
 * Data Access Object para la entidad CredencialAcceso.
 * Gestiona todas las operaciones de persistencia de credenciales en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<CredencialAcceso> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Relaciones:
 * - Esta entidad tiene una relación 1 a 1 (UNIQUE y NOT NULL) con Usuario.
 * - Generalmente, una CredencialAcceso se crea y elimina junto con su Usuario.
 *
 */
public class CredencialAccesoDAO implements GenericDAO<CredencialAcceso> {
    /**
     * Query de inserción de credencial de acceso.
     * Inserta hashPassword, salt, ultimoCambio y requireReset.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     * El campo eliminado tiene DEFAULT FALSE en la BD.
     */
    private static final String INSERT_SQL = "INSERT INTO credencial_acceso (hashPassword, salt, ultimoCambio, requireReset) VALUES (?, ?, ?, ?)";

    /**
     * Query de actualización de credencial de acceso.
     * Actualiza todos los campos editables por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     */
    private static final String UPDATE_SQL = "UPDATE credencial_acceso SET hashPassword = ?, salt = ?, ultimoCambio = ?, requireReset = ? WHERE id = ?";

    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay un usuario asociado.
     * La tabla 'usuarios' tiene una FK (credencial_id) que es NOT NULL y UNIQUE,
     * y con ON DELETE RESTRICT.
     *
     * Un soft delete (UPDATE) NO será bloqueado por la FK, pero dejará al
     * usuario asociado apuntando a una credencial "eliminada", causando
     * inconsistencias.
     *
     * Este DAO asume que el Servicio (Service) gestionará la eliminación
     * coordinada del Usuario y su Credencial.
     */
    private static final String DELETE_SQL = "UPDATE credencial_acceso SET eliminado = TRUE WHERE id = ?";

    /**
     * Query para obtener credencial por ID.
     * Solo retorna credenciales activas (eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM credencial_acceso WHERE id = ? AND eliminado = FALSE";

    /**
     * Query para obtener todas las credenciales activas.
     * Filtra por eliminado=FALSE.
     */
    private static final String SELECT_ALL_SQL = "SELECT * FROM credencial_acceso WHERE eliminado = FALSE";

    
    private static final String RECOVER_SQL = "UPDATE credencial_acceso SET eliminado = FALSE WHERE id = ?";
    
    /**
     * Inserta una credencial en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (hash, salt, etc.)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a credencial.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * IMPORTANTE: El ID generado se asigna al objeto credencial.
     * Esto permite que UsuarioServiceImpl.insertar() use credencial.getId()
     * inmediatamente después de insertar.
     *
     * @param credencial CredencialAcceso a insertar (id será ignorado y regenerado)
     * @throws SQLException Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(CredencialAcceso credencial) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setCredencialParameters(stmt, credencial);
            stmt.executeUpdate();

            setGeneratedId(stmt, credencial);
        }
    }

    /**
     * Inserta una credencial dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por:
     * - Servicios que insertan Usuario y CredencialAcceso de forma atómica.
     *
     * @param credencial CredencialAcceso a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertarTx(CredencialAcceso credencial, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setCredencialParameters(stmt, credencial);
            stmt.executeUpdate();
            setGeneratedId(stmt, credencial);
        }
    }

    /**
     * Actualiza una credencial existente en la base de datos.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La credencial no existe o ya está eliminada
     *
     * @param credencial CredencialAcceso con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void actualizar(CredencialAcceso credencial) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, credencial.getHashPassword());
            stmt.setString(2, credencial.getSalt());
            // Usar setObject para tipos java.time (LocalDateTime -> DATETIME)
            stmt.setObject(3, credencial.getUltimoCambio());
            stmt.setBoolean(4, credencial.isRequireReset());
            stmt.setLong(5, credencial.getId()); // ID va al final para el WHERE

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar la credencial con ID: " + credencial.getId());
            }
        }
    }

    /**
     * Actualiza una credencial existente dentro de una transacción.
     * NO crea ni cierra la conexión.
     *
     * @param credencial CredencialAcceso con los datos actualizados
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void actualizarTx(CredencialAcceso credencial, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, credencial.getHashPassword());
            stmt.setString(2, credencial.getSalt());
            stmt.setObject(3, credencial.getUltimoCambio());
            stmt.setBoolean(4, credencial.isRequireReset());
            stmt.setLong(5, credencial.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar la credencial con ID: " + credencial.getId());
            }
        }
    }
    
    /**
     * Elimina lógicamente una credencial (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La credencial no existe o ya está eliminada
     *
     * ⚠️ PELIGRO: Ver la advertencia en la query DELETE_SQL. Este método
     * debe ser llamado por un servicio que gestione la desvinculación
     * o eliminación del Usuario asociado.
     *
     * @param id ID de la credencial a eliminar (tipo long)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void eliminar(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró credencial con ID: " + id);
            }
        }
    }

        /**
     * Elimina lógicamente una credencial (soft delete) dentro de una transacción.
     * NO crea ni cierra la conexión.
     *
     * @param id ID de la credencial a eliminar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void eliminarTx(long id, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró credencial con ID: " + id);
            }
        }
    }
    
        /**
     * RECUPERA lógicamente una credencial (soft delete).
     * Marca eliminado=FALSE .
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La credencial no existe o no está eliminada
     *
     * @param id ID de la credencial a recuperar (tipo long)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void recuperar(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(RECOVER_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró credencial con ID: " + id);
            }
        }
    }

        /**
     * Recupera lógicamente una credencial dentro de una transacción.
     * NO crea ni cierra la conexión.
     *
     * @param id ID de la credencial a recuperar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws SQLException Si la credencial no existe o hay error de BD
     */
    @Override
    public void recuperarTx(long id, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(RECOVER_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró credencial con ID: " + id);
            }
        }
    }
    
    
    /**
     * Obtiene una credencial por su ID.
     * Solo retorna credenciales activas (eliminado=FALSE).
     *
     * @param id ID de la credencial a buscar (tipo long)
     * @return Credencial encontrada, o null si no existe o está eliminada
     * @throws SQLException Si hay error de BD
     */
    @Override
    public CredencialAcceso getById(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCredencial(rs);
                }
            }
        }
        return null;
    }

    /**
     * Obtiene todas las credenciales activas (eliminado=FALSE).
     *
     * @return Lista de credenciales activas (puede estar vacía)
     * @throws SQLException Si hay error de BD
     */
    @Override
    public List<CredencialAcceso> getAll() throws Exception {
        List<CredencialAcceso> credenciales = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                credenciales.add(mapResultSetToCredencial(rs));
            }
        }

        return credenciales;
    }

    /**
     * Setea los parámetros de la credencial en un PreparedStatement (para INSERT).
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados:
     * 1. hashPassword (String)
     * 2. salt (String)
     * 3. ultimoCambio (LocalDateTime)
     * 4. requireReset (boolean)
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param credencial Credencial con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setCredencialParameters(PreparedStatement stmt, CredencialAcceso credencial) throws SQLException {
        stmt.setString(1, credencial.getHashPassword());
        stmt.setString(2, credencial.getSalt());
        stmt.setObject(3, credencial.getUltimoCambio());
        stmt.setBoolean(4, credencial.isRequireReset());
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado (tipo long) al objeto credencial.
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param credencial Objeto credencial a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado
     */
    private void setGeneratedId(PreparedStatement stmt, CredencialAcceso credencial) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                credencial.setId(generatedKeys.getLong(1)); // Usar getLong para BIGINT
            } else {
                throw new SQLException("La inserción de la credencial falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto CredencialAcceso.
     *
     * @param rs ResultSet posicionado en una fila con datos de credencial
     * @return CredencialAcceso reconstruido
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private CredencialAcceso mapResultSetToCredencial(ResultSet rs) throws SQLException {
        CredencialAcceso credencial = new CredencialAcceso();
        
        credencial.setId(rs.getLong("id"));
        credencial.setHashPassword(rs.getString("hashPassword"));
        credencial.setSalt(rs.getString("salt"));
        // Mapear DATETIME/TIMESTAMP a LocalDateTime
        credencial.setUltimoCambio(rs.getObject("ultimoCambio", LocalDateTime.class)); 
        credencial.setRequireReset(rs.getBoolean("requireReset"));
        
        return credencial;
    }
}
