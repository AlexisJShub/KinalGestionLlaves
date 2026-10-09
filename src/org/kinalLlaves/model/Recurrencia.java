package org.kinalLlaves.model;

import java.time.LocalDateTime;

public record Recurrencia(long id, long salonId, long empleadoId, LocalDateTime inicio, LocalDateTime fin, String estado) {

}
