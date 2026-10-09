package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.LlaveEstadoDAO;
import org.kinalllaves.util.*;
import java.sql.*;
import java.util.Set;

public final class LlaveEstadoDAOImpl implements LlaveEstadoDAO {

    @Override
    public void cambiarEstado(long idLlave, String estado) throws SQLException {
        Permisos.exigir("LLAVES");
        if (idLlave <= 0) {
            throw new SQLException("Selecciona primero una llave");
        }
        if (!Set.of("DISPONIBLE", "DANADA", "EXTRAVIADA", "FUERA_SERVICIO").contains(estado)) {
            throw new SQLException("Estado de llave no permitido");
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT estado FROM llaves WHERE id_llave=? AND activo=1 FOR UPDATE")) {
                    lock.setLong(1, idLlave);
                    try (ResultSet r = lock.executeQuery()) {
                        if (!r.next()) {
                            throw new SQLException("Llave inexistente o inactiva");
                        }
                    }
                }
                try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM entregas WHERE id_llave=? AND estado='ACTIVA'")) {
                    check.setLong(1, idLlave);
                    try (ResultSet r = check.executeQuery()) {
                        r.next();
                        if (r.getInt(1) > 0) {
                            throw new SQLException("No cambies el estado mientras hay una entrega activa");
                        }
                    }
                }
                try (PreparedStatement up = c.prepareStatement("UPDATE llaves SET estado=? WHERE id_llave=?")) {
                    up.setString(1, estado);
                    up.setLong(2, idLlave);
                    up.executeUpdate();
                }
                try (PreparedStatement audit = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle) VALUES(?,'ESTADO_LLAVE','llaves',?,?)")) {
                    audit.setLong(1, Sesion.id());
                    audit.setLong(2, idLlave);
                    audit.setString(3, "Estado manual: " + estado);
                    audit.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException ex) {
                    throw ex;
                }
                throw new SQLException("Error de estado", e);
            }
        }
    }
}
