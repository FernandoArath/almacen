package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.PermisoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.Permiso;
import util.Http;

@WebServlet("/api/permisos/*")
public class PermisoServlet extends HttpServlet {

    private final PermisoDAO dao = new PermisoDAO();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    private Integer leerId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] ids = Http.ids(req);
        if (ids.length == 0) {
            Http.error(resp, 400, "Falta el id en la URL");
            return null;
        }
        try {
            return Integer.parseInt(ids[0]);
        } catch (NumberFormatException e) {
            Http.error(resp, 400, "El id debe ser un número");
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (Http.ids(req).length == 0) {
                Http.enviar(resp, 200, dao.listar());
                return;
            }
            Integer id = leerId(req, resp);
            if (id == null) {
                return;
            }
            Permiso p = dao.buscarPorId(id);
            if (p == null) {
                Http.error(resp, 404, "Permiso no encontrado");
            } else {
                Http.enviar(resp, 200, p);
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Permiso p = Http.MAPPER.readValue(req.getInputStream(), Permiso.class);
            if (p.getNombre() == null || p.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Http.enviar(resp, 201, dao.crear(p));
        } catch (JsonProcessingException e) {
            Http.error(resp, 400, "JSON inválido");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = leerId(req, resp);
        if (id == null) {
            return;
        }
        try {
            Permiso p = Http.MAPPER.readValue(req.getInputStream(), Permiso.class);
            if (p.getNombre() == null || p.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Permiso actualizado = dao.actualizar(id, p);
            if (actualizado == null) {
                Http.error(resp, 404, "Permiso no encontrado");
            } else {
                Http.enviar(resp, 200, actualizado);
            }
        } catch (JsonProcessingException e) {
            Http.error(resp, 400, "JSON inválido");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = leerId(req, resp);
        if (id == null) {
            return;
        }
        try {
            JsonNode campos = Http.MAPPER.readTree(req.getInputStream());
            Permiso actualizado = dao.actualizarParcial(id, campos);
            if (actualizado == null) {
                Http.error(resp, 404, "Permiso no encontrado");
            } else {
                Http.enviar(resp, 200, actualizado);
            }
        } catch (JsonProcessingException e) {
            Http.error(resp, 400, "JSON inválido");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = leerId(req, resp);
        if (id == null) {
            return;
        }
        try {
            if (dao.eliminar(id)) {
                Http.mensaje(resp, 200, "Permiso eliminado");
            } else {
                Http.error(resp, 404, "Permiso no encontrado");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}