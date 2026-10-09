package org.kinalLlaves.model;

import java.time.LocalDateTime;

public record Auditoria(long id, long usuarioId, String evento, String entidad, LocalDateTime fecha) {

}
