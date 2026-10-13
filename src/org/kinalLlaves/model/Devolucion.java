package org.kinalllaves.model;
import java.time.LocalDateTime;
public record Devolucion(long id,long entregaId,long quienDevuelve,LocalDateTime fecha) {}
