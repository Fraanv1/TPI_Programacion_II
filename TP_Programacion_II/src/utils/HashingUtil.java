package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad simple para hashear contraseñas con Salt.
 * Usa solo librerías nativas de Java (SHA-256 y SecureRandom).
 * Ideal para proyectos académicos donde no se quieren dependencias externas.
 */
public class HashingUtil {

    /**
     * Genera un salt aleatorio y seguro.
     * @return Un string de salt codificado en Base64.
     */
    public static String generarSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 16 bytes es un buen tamaño para un salt
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashea la contraseña combinándola con el salt.
     *
     * @param passwordTextoPlano La contraseña del usuario.
     * @param salt El salt generado (en Base64).
     * @return El hash de la contraseña (en Base64).
     */
    public static String hashPassword(String passwordTextoPlano, String salt) {
        try {
            // Se usa SHA-256, un estándar seguro y nativo de Java
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Combina el salt y la contraseña
            String passwordConSalt = passwordTextoPlano + salt;
            
            // Genera el hash
            byte[] hashBytes = md.digest(passwordConSalt.getBytes("UTF-8"));
            
            // Devuelve el hash como un string legible (Base64)
            return Base64.getEncoder().encodeToString(hashBytes);
            
        } catch (Exception e) {
            // En una app real, nunca deberíamos llegar aquí si SHA-256 existe
            throw new RuntimeException("Error al hashear la contraseña. Mensaje de error: " + e.getMessage());
        }
    }

}