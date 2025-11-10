/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.CredencialAcceso;
import model.Usuario;

/**
 * **Data Access Object (DAO) para la entidad Usuario.**
 * Implementa las operaciones CRUD y métodos de búsqueda específicos.
 * Gestiona la persistencia de objetos Usuario en la tabla 'usuarios'.
 *
 * @author soilu
 */
public class UsuarioDAO implements GenericDAO<Usuario> {
    
    private static final String INSERT_SQL = "INSERT INTO usuarios (username, email, credencial_id) VALUES (?, ?, ?)";

    /**
     * Query de actualización de **Usuario**. Actualiza username, email, estado activo y FK
     * credencial_id por id. NO actualiza el flag eliminado (solo se modifica en
     * soft delete).
     */
    private static final String UPDATE_SQL = "UPDATE usuarios SET username = ?, email = ?, activo = ?, credencial_id = ? WHERE id = ?";

    /**
     * Query de soft delete. Marca eliminado=TRUE sin borrar físicamente la
     * fila. Preserva integridad referencial y datos históricos.
     */
    private static final String DELETE_SQL = "UPDATE usuarios SET eliminado = TRUE WHERE id = ?";

    /**
     * Query para obtener **Usuario** por ID. LEFT JOIN con credencial_acceso para cargar
     * la relación de forma eager. Solo retorna usuarios activos
     * (eliminado=FALSE).
     *
     * Campos del ResultSet:
     * - Usuario: id, username, email, activo, fechaRegistro
     * - CredencialAcceso (puede ser NULL): credencial_id, hashPassword, salt, ultimoCambio, requireReset
     */
    private static final String SELECT_BY_ID_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.id = ? AND u.eliminado = FALSE";

    /**
     * Query para obtener todos los **Usuarios** activos. LEFT JOIN con credencial_acceso
     * para cargar relaciones. Filtra por eliminado=FALSE (solo usuarios
     * activos).
     */
    private static final String SELECT_ALL_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.eliminado = FALSE";

    /**
     * Query de búsqueda por **username** con . Permite búsqueda solo con usuarios no eliminados
     * (eliminado=FALSE).
     */
    private static final String SEARCH_BY_USERNAME_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.eliminado = FALSE AND u.username = ?";

    /**
     * Query de búsqueda exacta por **EMAIL**. Usa comparación exacta (=) porque el
     * email debe ser único.
     * Solo usuarios activos (eliminado=FALSE).
     */
    private static final String SEARCH_BY_EMAIL_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.eliminado = FALSE AND u.email = ?";

    
    private static final String RECOVER_SQL = "UPDATE usuarios SET eliminado = FALSE WHERE id = ?";
    
    /**
     * DAO de CredencialAcceso. Usado para operaciones que puedan requerir
     * la coordinación de la persistencia de las credenciales.
     */
    private final CredencialAccesoDAO credencialAccesoDAO;
    
    
    /**
     * Constructor con inyección de CredencialAccesoDAO.
     * Valida que la dependencia no sea null (fail-fast).
     *
     * @param credencialAccesoDAO DAO de CredencialAcceso
     * @throws IllegalArgumentException si credencialAccesoDAO es null
     */
    public UsuarioDAO(CredencialAccesoDAO credencialAccesoDAO) {
        if (credencialAccesoDAO == null) {
            throw new IllegalArgumentException("credencialAccesoDAO no puede ser null");
        }
        this.credencialAccesoDAO = credencialAccesoDAO;
    }
    
        /**
     * Inserta un **Usuario** en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (username, email, credencial_id)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a usuario.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * @param usuario Usuario a insertar (id será ignorado y regenerado)
     * @throws Exception Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Usuario usuario) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setUsuarioParameters(stmt, usuario);
            stmt.executeUpdate();
            setGeneratedId(stmt, usuario);
        }
    }

    /**
     * Inserta un **Usuario** dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por:
     * - Operaciones que requieren múltiples inserts coordinados (ej: Usuario + CredencialAcceso)
     * - Rollback automático si alguna operación falla
     *
     * @param usuario Usuario a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertarTx(Usuario usuario, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setUsuarioParameters(stmt, usuario);
            stmt.executeUpdate();
            setGeneratedId(stmt, usuario);
        }
    }

    /**
     * Actualiza un **Usuario** existente en la base de datos.
     * Actualiza username, email, estado activo y FK credencial_id.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El usuario no existe o ya está eliminado
     *
     * IMPORTANTE: Este método puede cambiar la FK credencial_id:
     * - Si usuario.credencial == null → credencial_id = NULL (desasociar)
     * - Si usuario.credencial.id > 0 → credencial_id = credencial.id (asociar/cambiar)
     *
     * @param usuario Usuario con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si el usuario no existe o hay error de BD
     */
    @Override
    public void actualizar(Usuario usuario) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, usuario.getEmail());
            stmt.setBoolean(3, usuario.isActivo());
            setCredencialAccesoId(stmt, 4, usuario.getCredencial());
            stmt.setLong(5, usuario.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el usuario con ID: " + usuario.getId());
            }
        }
    }
    
        /**
     * Actualiza un **Usuario** dentro de una transacción existente.
     * NO crea ni cierra la conexión.
     *
     * @param usuario Usuario con los datos actualizados
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la actualización
     */
    @Override
    public void actualizarTx(Usuario usuario, Connection conn) throws Exception {
        // Se usa la 'conn' que viene por parámetro, sin 'try-with-resources' para la conexión
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, usuario.getEmail());
            stmt.setBoolean(3, usuario.isActivo());
            setCredencialAccesoId(stmt, 4, usuario.getCredencial());
            stmt.setLong(5, usuario.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el usuario con ID: " + usuario.getId());
            }
        }
    }
    
    /**
     * Elimina lógicamente un **Usuario** (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El usuario no existe o ya está eliminado
     *
     * @param id ID del usuario a eliminar
     * @throws SQLException Si el usuario no existe o hay error de BD
     */
    @Override
    public void eliminar(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }
        }
    }
    
        /**
     * Elimina lógicamente un **Usuario** (soft delete) dentro de una transacción.
     * NO crea ni cierra la conexión.
     *
     * @param id ID del usuario a eliminar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si el usuario no existe o hay error de BD
     */
    @Override
    public void eliminarTx(long id, Connection conn) throws Exception {
        // Se usa la conn que viene por parámetro
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }
        }
    }

    /**
     * Recupera lógicamente un usuario
     * Marca eliminado=FALSE
     *
     * Validaciones: - Si rowsAffected == 0 → El usuario no existe o no está eliminado
     *
     * @param id ID del usuario a recuperar
     * @throws SQLException Si el usuario no existe o hay error de BD
     */
    @Override
    public void recuperar(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(RECOVER_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }
        }
    }
    
    /**
     * Recupera lógicamente un Usuario dentro de una
     * transacción. NO crea ni cierra la conexión.
     *
     * @param id ID del usuario a recuperar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si el usuario no existe o hay error de BD
     */
    @Override
    public void recuperarTx(long id, Connection conn) throws Exception {
        // Se usa la conn que viene por parámetro
        try (PreparedStatement stmt = conn.prepareStatement(RECOVER_SQL)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + id);
            }
        }
    }
    
    
    /**
     * Obtiene un **Usuario** por su ID.
     * Incluye su CredencialAcceso asociado mediante LEFT JOIN.
     *
     * @param id ID del usuario a buscar
     * @return Usuario encontrado con su credencial, o null si no existe o está eliminado
     * @throws Exception Si hay error de BD (captura SQLException y re-lanza con mensaje descriptivo)
     */
    @Override
    public Usuario getById(long id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener usuario por ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Obtiene todos los **Usuarios** activos (eliminado=FALSE).
     * Incluye sus Credenciales de Acceso mediante LEFT JOIN.
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * @return Lista de usuarios activos con sus credenciales (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Usuario> getAll() throws Exception {
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todos los usuarios: " + e.getMessage(), e);
        }
        return usuarios;
    }

 /**
     * Busca un **Usuario** por **username** exacto.
     * Usa comparación exacta (=) ya que el username es ÚNICO.
     *
     * @param username Username exacto a buscar (no puede estar vacío)
     * @return El Usuario encontrado, o null si no existe o está eliminado
     * @throws IllegalArgumentException Si el username está vacío
     * @throws SQLException Si hay error de BD
     */
    
    
    public Usuario buscarPorUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username de búsqueda no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_USERNAME_SQL)) {

            // Seteamos el username exacto
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                // Si encontramos un resultado (debe ser único)
                if (rs.next()) {
                    // Mapeamos y devolvemos el usuario
                    return mapResultSetToUsuario(rs);
                }
            }
        }
        
        // Si no se encontró ningún usuario con ese username
        return null;
    }

    /**
     * Busca un **Usuario** por **EMAIL** exacto.
     * Usa comparación exacta (=) ya que el email debe ser único.
     *
     * Uso típico:
     * - Validar que el email no esté duplicado
     * - Buscar un usuario específico para login
     *
     * @param email Email exacto a buscar (se aplica trim automáticamente)
     * @return Usuario con ese email, o null si no existe o está eliminado
     * @throws IllegalArgumentException Si el email está vacío
     * @throws SQLException Si hay error de BD
     */
    public Usuario buscarPorEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_EMAIL_SQL)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Setea los parámetros de **Usuario** en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados:
     * 1. username (String)
     * 2. email (String)
     * 3. credencial_id (Integer o NULL)
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param usuario Usuario con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setUsuarioParameters(PreparedStatement stmt, Usuario usuario) throws SQLException {
        stmt.setString(1, usuario.getUsername());
        stmt.setString(2, usuario.getEmail());
        setCredencialAccesoId(stmt, 3, usuario.getCredencial());
    }

    /**
     * Setea la FK credencial_id en un PreparedStatement.
     * Maneja correctamente el caso NULL (usuario sin credencial asociada).
     *
     * Lógica:
     * - Si credencial != null Y credencial.id > 0 → Setea el ID
     * - Si credencial == null O credencial.id <= 0 → Setea NULL
     *
     * Importante: El tipo Types.INTEGER es necesario para setNull() en JDBC.
     *
     * @param stmt PreparedStatement
     * @param parameterIndex Índice del parámetro (1-based)
     * @param credencial CredencialAcceso asociada (puede ser null)
     * @throws SQLException Si hay error al setear el parámetro
     */
    private void setCredencialAccesoId(PreparedStatement stmt, int parameterIndex, CredencialAcceso credencial) throws SQLException {
        if (credencial != null && credencial.getId() > 0) {
            stmt.setLong(parameterIndex, credencial.getId());
        } else {
            stmt.setNull(parameterIndex, Types.INTEGER);
        }
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto usuario.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto usuario debe tener su ID real de la BD
     * - Permite usar usuario.getId() inmediatamente después de insertar
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param usuario Objeto usuario a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Usuario usuario) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                usuario.setId(generatedKeys.getLong(1));
            } else {
                throw new SQLException("La inserción del usuario falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto **Usuario**.
     * Reconstruye la relación con **CredencialAcceso** usando LEFT JOIN.
     *
     * Mapeo de columnas:
     * Usuario:
     * - id → u.id
     * - username → u.username
     * - email → u.email
     * - activo → u.activo
     *
     * CredencialAcceso (puede ser NULL si el usuario no tiene credencial):
     * - id → c_a.id AS credencial_id
     * - hashPassword → c_a.hashPassword
     * - salt → c_a.salt
     * - requireReset → c_a.requireReset
     * - ultimoCambio → c_a.ultimoCambio
     *
     * @param rs ResultSet posicionado en una fila con datos de usuario y credencial
     * @return Usuario reconstruido con su credencial (si tiene)
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setUsername(rs.getString("username"));
        usuario.setEmail(rs.getString("email"));
        usuario.setActivo(rs.getBoolean("activo"));
        usuario.setFechaRegistro(rs.getObject("fechaRegistro", LocalDateTime.class)); // Agregado mapeo de fechaRegistro

        // Manejo correcto de LEFT JOIN: verificar si credencial_id es NULL
        long credencialAccesoId = rs.getLong("credencial_id");
        if (!rs.wasNull()) { // Si credencial_id es un valor real (no NULL)
            CredencialAcceso credencial = new CredencialAcceso();
            credencial.setId(credencialAccesoId);
            credencial.setHashPassword(rs.getString("hashPassword"));
            credencial.setSalt(rs.getString("salt"));
            credencial.setRequireReset(rs.getBoolean("requireReset"));
            // La columna ultimoCambio es de tipo DATETIME en la BD
            credencial.setUltimoCambio(rs.getObject("ultimoCambio", LocalDateTime.class)); 
            usuario.setCredencial(credencial);
        }

        return usuario;
    }
    
    
    
}