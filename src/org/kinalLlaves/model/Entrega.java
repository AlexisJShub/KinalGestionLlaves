package org.kinalllaves.model;
import java.time.LocalDateTime;
public record Entrega(long id,long llaveId,long empleadoId,LocalDateTime fecha,String estado) {}
