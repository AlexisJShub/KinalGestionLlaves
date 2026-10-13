package org.kinalllaves.util;

public final class Permisos {
    private Permisos() {}

    public static boolean puede(String modulo) {
        return Sesion.actual() != null
                && modulo != null
                && Sesion.permisos().contains(modulo);
    }

    public static void exigir(String modulo) {
        if (!puede(modulo)) {
            throw new SecurityException("No tienes permiso para acceder a " + modulo + ".");
        }
    }
}
