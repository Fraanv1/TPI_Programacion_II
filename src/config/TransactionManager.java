/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author soilu
 */
public class TransactionManager implements AutoCloseable {

        private Connection conn;
        private boolean transactionActive;

        // Se conecta si la conexión no es nula
        public TransactionManager(Connection conn) throws SQLException {
            if (conn == null) {
                throw new IllegalArgumentException("La conexión no puede ser null");
            }
            this.conn = conn;
            this.transactionActive = false;
        }
        
        public Connection getConnection() {
            return conn;
        }
        
        // Inicia la transacción si la conexión está disponible, y setea el autocommit en false, para poder manejar nosotros cuando se hace commit, y cuando rollback
        public void startTransaction() throws SQLException {
            if (conn == null) {
                throw new SQLException("No se puede iniciar la transacción: conexión no disponible");
            }
            if (conn.isClosed()) {
                throw new SQLException("No se puede iniciar la transacción: conexión cerrada");
            }
            conn.setAutoCommit(false);
            transactionActive = true;
        }

        // Función que valida si hay conexión y si la transacción está activa, para luego hacer el commit
        public void commit() throws SQLException {
            if (conn == null) {
                throw new SQLException("Error al hacer commit: no hay conexión establecida");
            }
            if (!transactionActive) {
                throw new SQLException("No hay una transacción activa para hacer commit");
            }
            conn.commit();
            transactionActive = false;
        }

        // Función que valida si la conexión no es nula y si la transacción está activa, para luego intentar un rollback, usando un try catch por  si es que algo sale mal
        public void rollback() {
            if (conn != null && transactionActive) {
                try {
                    System.out.println("Error durante la transacción, realizando rollback...");
                    conn.rollback();
                    transactionActive = false;
                } catch (SQLException e) {
                    System.err.println("Error durante el rollback: " + e.getMessage());
                }
            }
        }

        // Sobreescribe la función de la interfaz "AutoCloseable", validando si la conexión no es nula, intenta ver si la transacción está activa, y de ser asi hacer un rollback. Siempre setea autocommit en true, y cierra la conexión
        // Usando try-catch por si hay un error al cerrar la conexión.
        @Override
        public void close() {
            if (conn != null) {
                try {
                    if (transactionActive) {
                        rollback();
                    }
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }

        
        public boolean isTransactionActive() {
            return transactionActive;
        }
    }


