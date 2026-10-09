package org.kinalLlaves.dao.impl;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import org.kinalLlaves.dao.ReservaDAO;
import org.kinalllaves.util.*;

public class ReservaDAOImpl implements ReservaDAO {
    public void reservar(long salon,long empleado,LocalDateTime inicio,LocalDateTime fin,int semanas,String obs)throws SQLException {
        Permisos.exigir("RESERVAS");
        if (inicio==null || fin==null || !fin.isAfter(inicio) || semanas<1 || semanas>16)
            throw new SQLException("Revisa la fecha, las horas y el número de semanas.");
        if (inicio.isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new SQLException("No puedes reservar una fecha pasada.");
        try(Connection c=Conexion.getInstancia().conectar()){
            c.setAutoCommit(false);
            try {
                try(PreparedStatement lock=c.prepareStatement("SELECT id_salon FROM salones WHERE id_salon=? AND activo=1 FOR UPDATE")){
                    lock.setLong(1,salon);
                    try(ResultSet r=lock.executeQuery()){
                        if(!r.next())throw new SQLException("Ese salón ya no está disponible.");
                    }
                }
                try(PreparedStatement check=c.prepareStatement("SELECT COUNT(*) FROM llaves WHERE id_salon=? AND activo=1 AND estado NOT IN ('EXTRAVIADA','DANADA','FUERA_SERVICIO')")){
                    check.setLong(1,salon);
                    try(ResultSet r=check.executeQuery()){
                        r.next();if(r.getInt(1)!=1)throw new SQLException("El salón no tiene una llave en buen estado.");
                    }
                }
                try(PreparedStatement check=c.prepareStatement("SELECT COUNT(*) FROM empleados WHERE id_empleado=? AND activo=1")){
                    check.setLong(1,empleado);
                    try(ResultSet r=check.executeQuery()){
                        r.next();if(r.getInt(1)!=1)throw new SQLException("El empleado no está activo.");
                    }
                }
                for(int n=0;n<semanas;n++){
                    Timestamp a=Timestamp.valueOf(inicio.plusWeeks(n));
                    Timestamp b=Timestamp.valueOf(fin.plusWeeks(n));
                    try(PreparedStatement check=c.prepareStatement("SELECT COUNT(*) FROM reservas WHERE id_salon=? AND estado='ACTIVA' AND inicio<? AND fin>?")){
                        check.setLong(1,salon);check.setTimestamp(2,b);check.setTimestamp(3,a);
                        try(ResultSet r=check.executeQuery()){
                            r.next();if(r.getInt(1)>0)throw new SQLException("Ya hay una reserva en la semana " +(n+1)+". Cambia el horario.");
                        }
                    }
                }
                Long serie=null;
                if(semanas>1){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO series_reserva(id_usuario) VALUES(?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setLong(1,Sesion.id());p.executeUpdate();
                        try(ResultSet r=p.getGeneratedKeys()){
                            if(!r.next())throw new SQLException("No se pudo registrar la serie.");
                            serie=r.getLong(1);
                        }
                    }
                }
                for(int n=0;n<semanas;n++){
                    long id;
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO reservas(id_salon,id_empleado,id_usuario,id_serie,inicio,fin,observacion) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setLong(1,salon);p.setLong(2,empleado);p.setLong(3,Sesion.id());
                        if(serie==null)p.setNull(4,Types.BIGINT);else p.setLong(4,serie);
                        p.setTimestamp(5,Timestamp.valueOf(inicio.plusWeeks(n)));
                        p.setTimestamp(6,Timestamp.valueOf(fin.plusWeeks(n)));
                        p.setString(7,obs);p.executeUpdate();
                        try(ResultSet r=p.getGeneratedKeys()){
                            if(!r.next())throw new SQLException("No se pudo registrar la fecha.");
                            id=r.getLong(1);
                        }
                    }
                    try(PreparedStatement audit=c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad) VALUES(?,'RESERVA','reservas',?)")){
                        audit.setLong(1,Sesion.id());audit.setLong(2,id);audit.executeUpdate();
                    }
                }
                c.commit();
            } catch(Exception ex) {
                c.rollback();
                if(ex instanceof SQLException sql)throw sql;
                throw new SQLException("No se pudo guardar la reserva: " + ex.getMessage(),ex);
            }
        }
    }

    public void cambiarEstado(long id,String estado)throws SQLException {
        Permisos.exigir("RESERVAS");
        if(!Set.of("CANCELADA","CERRADA").contains(estado))throw new SQLException("Estado de reserva incorrecto.");
        try(Connection c=Conexion.getInstancia().conectar()){
            c.setAutoCommit(false);
            try {
                Long serie=null;
                try(PreparedStatement p=c.prepareStatement("SELECT id_serie FROM reservas WHERE id_reserva=? AND estado='ACTIVA' FOR UPDATE")){
                    p.setLong(1,id);
                    try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("La reserva ya no está activa.");
                        long valor=r.getLong(1);if(!r.wasNull())serie=valor;
                    }
                }
                String query=serie==null
                    ? "UPDATE reservas SET estado=? WHERE id_reserva=? AND estado='ACTIVA'"
                    : "UPDATE reservas SET estado=? WHERE id_serie=? AND estado='ACTIVA'";
                try(PreparedStatement p=c.prepareStatement(query)){
                    p.setString(1,estado);p.setLong(2,serie==null?id:serie);
                    if(p.executeUpdate()<1)throw new SQLException("No se actualizó ninguna reserva.");
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle) VALUES(?,'RESERVA_ESTADO','reservas',?,?)")){
                    p.setLong(1,Sesion.id());p.setLong(2,id);
                    p.setString(3,estado + (serie!=null?" (serie semanal)":""));
                    p.executeUpdate();
                }
                c.commit();
            } catch(Exception ex) {
                c.rollback();
                if(ex instanceof SQLException sql)throw sql;
                throw new SQLException("No se pudo modificar la reserva.",ex);
            }
        }
    }

    public List<Map<String,Object>> listar()throws SQLException {
        Permisos.exigir("RESERVAS");
        String sql="SELECT MIN(r.id_reserva) id_reserva,s.codigo salon,"+
            "CONCAT(e.nombres,' ',e.apellidos) empleado, MIN(r.inicio) inicio, MAX(r.fin) fin,"+
            "COUNT(*) semanas, r.estado, MAX(r.observacion) observacion " +
            "FROM reservas r JOIN salones s ON r.id_salon=s.id_salon " +
            "JOIN empleados e ON r.id_empleado=e.id_empleado " +
            "GROUP BY COALESCE(r.id_serie,-r.id_reserva),s.codigo,e.nombres,e.apellidos,r.estado " +
            "ORDER BY inicio DESC LIMIT 350";
        List<Map<String,Object>> result=new ArrayList<>();
        try(Connection c=Conexion.getInstancia().conectar();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            ResultSetMetaData md=r.getMetaData();
            while(r.next()){
                Map<String,Object> fila=new LinkedHashMap<>();
                for(int i=1;i<=md.getColumnCount();i++)fila.put(md.getColumnLabel(i),r.getObject(i));
                result.add(fila);
            }
        }
        return result;
    }
}
