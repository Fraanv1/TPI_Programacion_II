/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.CredencialAcceso;
import model.Usuario;

/**
 *
 * @author soilu
 */
public class UsuarioDAO implements GenericDAO<Usuario> {
    
    private static final String INSERT_SQL = "INSERT INTO usuarios (username, email, credencial_id) VALUES (?, ?, ?)";

    /**
     * Query de actualización de persona. Actualiza nombre, apellido, dni y FK
     * domicilio_id por id. NO actualiza el flag eliminado (solo se modifica en
     * soft delete).
     */
    private static final String UPDATE_SQL = "UPDATE usuarios SET username = ?, email = ?, activo = ?, credencial_id = ? WHERE id = ?";

    /**
     * Query de soft delete. Marca eliminado=TRUE sin borrar físicamente la
     * fila. Preserva integridad referencial y datos históricos.
     */
    private static final String DELETE_SQL = "UPDATE usuarios SET eliminado = TRUE WHERE id = ?";

    /**
     * Query para obtener persona por ID. LEFT JOIN con domicilios para cargar
     * la relación de forma eager. Solo retorna personas activas
     * (eliminado=FALSE).
     *
     * Campos del ResultSet: - Persona: id, nombre, apellido, dni, domicilio_id
     * - Domicilio (puede ser NULL): dom_id, calle, numero
     */
    private static final String SELECT_BY_ID_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.id = ? AND u.eliminado = FALSE";

    /**
     * Query para obtener todas las personas activas. LEFT JOIN con domicilios
     * para cargar relaciones. Filtra por eliminado=FALSE (solo personas
     * activas).
     */
    private static final String SELECT_ALL_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.eliminado = FALSE";

    /**
     * Query de búsqueda por username  con LIKE. Permite búsqueda
     * flexible: el usuario ingresa "juan" y encuentra "Juan", "Juana", etc. Usa
     * % antes y después del filtro: LIKE '%filtro%' Solo personas activas
     * (eliminado=FALSE).
     */
    private static final String SEARCH_BY_USERNAME_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE p.eliminado = FALSE AND u.username LIKE ?";

    /**
     * Query de búsqueda exacta por EMAIL. Usa comparación exacta (=) porque el
     * DNI es único (RN-001). Usado por PersonaServiceImpl.validateDniUnique()
     * para verificar unicidad. Solo personas activas (eliminado=FALSE).
     */
    private static final String SEARCH_BY_EMAIL_SQL = "SELECT u.id, u.username, u.email, u.activo, u.fechaRegistro, "
            + "c_a.id AS credencial_id, c_a.hashPassword, c_a.salt, c_a.ultimoCambio, c_a.requireReset "
            + "FROM usuarios u LEFT JOIN credencial_acceso as c_a ON u.credencial_id = c_a.id "
            + "WHERE u.eliminado = FALSE AND u.email = ?";

    /**
     * DAO de domicilios (actualmente no usado, pero disponible para operaciones
     * futuras). Inyectado en el constructor por si se necesita coordinar
     * operaciones.
     */
    private final CredencialAccesoDAO credencialAccesoDAO;
    
    
    /**
     * Constructor con inyección de DomicilioDAO.
     * Valida que la dependencia no sea null (fail-fast).
     *
     * @param credencialAccesoDAO DAO de domicilios
     * @throws IllegalArgumentException si domicilioDAO es null
     */
    public UsuarioDAO(CredencialAccesoDAO credencialAccesoDAO) {
        if (credencialAccesoDAO == null) {
            throw new IllegalArgumentException("credencialAccesoDAO no puede ser null");
        }
        this.credencialAccesoDAO = credencialAccesoDAO;
    }
    
        /**
     * Inserta una persona en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (nombre, apellido, dni, domicilio_id)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a persona.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * @param usuario Persona a insertar (id será ignorado y regenerado)
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
     * Inserta una persona dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: (Actualmente no usado, pero disponible para transacciones futuras)
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param usuario Persona a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Usuario usuario, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setUsuarioParameters(stmt, usuario);
            stmt.executeUpdate();
            setGeneratedId(stmt, usuario);
        }
    }

    /**
     * Actualiza una persona existente en la base de datos.
     * Actualiza nombre, apellido, dni y FK domicilio_id.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La persona no existe o ya está eliminada
     *
     * IMPORTANTE: Este método puede cambiar la FK domicilio_id:
     * - Si persona.domicilio == null → domicilio_id = NULL (desasociar)
     * - Si persona.domicilio.id > 0 → domicilio_id = domicilio.id (asociar/cambiar)
     *
     * @param usuario Persona con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si la persona no existe o hay error de BD
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
                throw new SQLException("No se pudo actualizar la persona con ID: " + usuario.getId());
            }
        }
    }

    /**
     * Elimina lógicamente una persona (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La persona no existe o ya está eliminada
     *
     * IMPORTANTE: NO elimina el domicilio asociado (correcto según RN-037).
     * Múltiples personas pueden compartir un domicilio.
     *
     * @param id ID de la persona a eliminar
     * @throws SQLException Si la persona no existe o hay error de BD
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
     * Obtiene una persona por su ID.
     * Incluye su domicilio asociado mediante LEFT JOIN.
     *
     * @param id ID de la persona a buscar
     * @return Persona encontrada con su domicilio, o null si no existe o está eliminada
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
     * Obtiene todas las personas activas (eliminado=FALSE).
     * Incluye sus domicilios mediante LEFT JOIN.
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * @return Lista de personas activas con sus domicilios (puede estar vacía)
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
     * Busca personas por nombre o apellido con búsqueda flexible (LIKE).
     * Permite búsqueda parcial: "juan" encuentra "Juan", "María Juana", etc.
     *
     * Patrón de búsqueda: LIKE '%filtro%' en nombre O apellido
     * Búsqueda case-sensitive en MySQL (depende de la collation de la BD).
     *
     * Ejemplo:
     * - filtro = "garcia" → Encuentra personas con nombre o apellido que contengan "garcia"
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de personas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws SQLException Si hay error de BD
     */
    public List<Usuario> buscarPorUsername(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }

        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_USERNAME_SQL)) {

            // Construye el patrón LIKE: %filtro%
            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapResultSetToUsuario(rs));
                }
            }
        }
        return usuarios;
    }

    /**
     * Busca una persona por DNI exacto.
     * Usa comparación exacta (=) porque el DNI es único en el sistema (RN-001).
     *
     * Uso típico:
     * - PersonaServiceImpl.validateDniUnique() para verificar que el DNI no esté duplicado
     * - MenuHandler opción 4 para buscar persona específica por DNI
     *
     * @param email DNI exacto a buscar (se aplica trim automáticamente)
     * @return Persona con ese DNI, o null si no existe o está eliminada
     * @throws IllegalArgumentException Si el DNI está vacío
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
     * Setea los parámetros de persona en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados:
     * 1. nombre (String)
     * 2. apellido (String)
     * 3. dni (String)
     * 4. domicilio_id (Integer o NULL)
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param usuario Persona con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setUsuarioParameters(PreparedStatement stmt, Usuario usuario) throws SQLException {
        stmt.setString(1, usuario.getUsername());
        stmt.setString(2, usuario.getEmail());
        setCredencialAccesoId(stmt, 3, usuario.getCredencial());
    }

    /**
     * Setea la FK domicilio_id en un PreparedStatement.
     * Maneja correctamente el caso NULL (persona sin domicilio).
     *
     * Lógica:
     * - Si domicilio != null Y domicilio.id > 0 → Setea el ID
     * - Si domicilio == null O domicilio.id <= 0 → Setea NULL
     *
     * Importante: El tipo Types.INTEGER es necesario para setNull() en JDBC.
     *
     * @param stmt PreparedStatement
     * @param parameterIndex Índice del parámetro (1-based)
     * @param credencial Domicilio asociado (puede ser null)
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
     * Asigna el ID generado al objeto persona.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto persona debe tener su ID real de la BD
     * - Permite usar persona.getId() inmediatamente después de insertar
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param usuario Objeto persona a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Usuario usuario) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                usuario.setId(generatedKeys.getLong(1));
            } else {
                throw new SQLException("La inserción de la persona falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Persona.
     * Reconstruye la relación con Domicilio usando LEFT JOIN.
     *
     * Mapeo de columnas:
     * Persona:
     * - id → p.id
     * - nombre → p.nombre
     * - apellido → p.apellido
     * - dni → p.dni
     *
     * Domicilio (puede ser NULL si la persona no tiene domicilio):
     * - id → d.id AS dom_id
     * - calle → d.calle
     * - numero → d.numero
     *
     * Lógica de NULL en LEFT JOIN:
     * - Si domicilio_id es NULL → persona.domicilio = null (correcto)
     * - Si domicilio_id > 0 → Se crea objeto Domicilio y se asigna a persona
     *
     * @param rs ResultSet posicionado en una fila con datos de persona y domicilio
     * @return Persona reconstruida con su domicilio (si tiene)
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setUsername(rs.getString("username"));
        usuario.setEmail(rs.getString("email"));
        usuario.setActivo(rs.getBoolean("activo"));

        // Manejo correcto de LEFT JOIN: verificar si domicilio_id es NULL
        int credencialAccesoId = rs.getInt("credencial_id");
        if (credencialAccesoId > 0 && !rs.wasNull()) {
            CredencialAcceso credencial = new CredencialAcceso();
            credencial.setId(rs.getLong("id"));
            credencial.setHashPassword(rs.getString("hashPassword"));
            credencial.setSalt(rs.getString("salt"));
            credencial.setRequireReset(rs.getBoolean("requireReset"));
            credencial.setUltimoCambio(LocalDateTime.now());
            usuario.setCredencial(credencial);
        }

        return usuario;
    }
    
    
    
}
