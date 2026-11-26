package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.RolDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.Rol;
import util.Http;

@WebServlet("/api/roles/*")
public class RolServlet extends HttpServlet {

    private final RolDAO dao = new RolDAO();

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
            Rol r = dao.buscarPorId(id);
            if (r == null) {
                Http.error(resp, 404, "Rol no encontrado");
            } else {
                Http.enviar(resp, 200, r);
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Rol r = Http.MAPPER.readValue(req.getInputStream(), Rol.class);
            if (r.getNombre() == null || r.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Http.enviar(resp, 201, dao.crear(r));
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
            Rol r = Http.MAPPER.readValue(req.getInputStream(), Rol.class);
            if (r.getNombre() == null || r.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Rol actualizado = dao.actualizar(id, r);
            if (actualizado == null) {
                Http.error(resp, 404, "Rol no encontrado");
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
            Rol actualizado = dao.actualizarParcial(id, campos);
            if (actualizado == null) {
                Http.error(resp, 404, "Rol no encontrado");
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
                Http.mensaje(resp, 200, "Rol eliminado");
            } else {
                Http.error(resp, 404, "Rol no encontrado");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}