/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.util.List;

/**
 *
 * @author soilu
 */
public interface GenericDAO<T> {
    
    void insertar(T entidad) throws Exception;
    void insertarTx(T entidad, Connection conn) throws Exception;

    void actualizar(T entidad) throws Exception;
    void actualizarTx(T entidad, Connection conn) throws Exception;
    
    void eliminar(long id) throws Exception;
    void eliminarTx(long id, Connection conn) throws Exception;
    
    void recuperar(long id) throws Exception;
    void recuperarTx(long id, Connection conn) throws Exception;
    
    T getById(long id) throws Exception;

    List<T> getAll() throws Exception;
    
}
