package org.kinalllaves.dao;

import java.sql.SQLException;
import org.kinalllaves.model.EmpleadoIdentificado;

public interface EmpleadoDAO {

    Long buscarPorRfid(String uid) throws SQLException;

    Long buscarPorCarnet(String carnet) throws SQLException;

    EmpleadoIdentificado identificarPorCarnet(String carnet) throws SQLException;
}
