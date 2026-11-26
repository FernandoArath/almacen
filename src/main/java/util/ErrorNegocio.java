package util;

public class ErrorNegocio extends Exception {

    private final int status;

    public ErrorNegocio(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}