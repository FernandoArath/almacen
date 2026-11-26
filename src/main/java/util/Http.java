package util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class Http {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static void enviar(HttpServletResponse resp, int status, Object objeto) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(resp.getWriter(), objeto);
    }

    public static void mensaje(HttpServletResponse resp, int status, String texto) throws IOException {
        enviar(resp, status, Map.of("mensaje", texto));
    }

    public static void error(HttpServletResponse resp, int status, String texto) throws IOException {
        enviar(resp, status, Map.of("error", texto));
    }

    public static void errorSql(HttpServletResponse resp, SQLException e) throws IOException {
        String estado = e.getSQLState();
        if ("23505".equals(estado)) {
            error(resp, 409, "Registro duplicado");
        } else if ("23503".equals(estado)) {
            error(resp, 409, "Referencia inexistente o registro en uso");
        } else if ("23502".equals(estado) || "23514".equals(estado)) {
            error(resp, 400, "Dato faltante o inválido");
        } else {
            e.printStackTrace();
            error(resp, 500, "Error en la base de datos");
        }
    }

    // Para /api/productos/3 devuelve ["3"]; sin id devuelve un arreglo vacío
    public static String[] ids(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) {
            return new String[0];
        }
        return info.substring(1).split("/");
    }
}