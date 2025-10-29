
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author Frappo
 */
public class Transactions {

    private Connection conn;

    public Transactions(Connection conn) {
        this.conn = conn;
    }

    public void insertar(String hash, String salt, Timestamp cambio, int requireReset) throws SQLException {
        String sql = "INSERT INTO credencial_acceso (eliminado, hashPassword, salt, ultimoCambio, requireReset) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 0);
            stmt.setString(2, hash);
            stmt.setString(3, salt);
            stmt.setTimestamp(4, cambio);
            stmt.setInt(5, requireReset);
            stmt.executeUpdate();
        }
    }
        
    public void actualizar(String hash, String salt, Timestamp cambio, int requireReset) throws SQLException {
        String sql = "UPDATE credencial_acceso (eliminado, hashPassword, salt, ultimoCambio, requireReset) WHERE id = VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 0);
            stmt.setString(2, hash);
            stmt.setString(3, salt);
            stmt.setTimestamp(4, cambio);
            stmt.setInt(5, requireReset);
            stmt.executeUpdate();
        }
    }
    
     public void eliminar(int id) throws SQLException {
        String sql = "DELETE * FROM WHERE ID = ";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 0);
        }
    }
     
     public String getById(){
         return "SELECT * FROM";
     }
     
     public String getAll(){
         return "SELECT * FROM Usuarios";
     }
}
