package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoriaDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.Categoria;
import util.Http;

@WebServlet("/api/categorias/*")
public class CategoriaServlets extends HttpServlet {

    private final CategoriaDao dao = new CategoriaDao();

    // HttpServlet no trae doPatch en Jakarta EE 10, así que lo enrutamos aquí
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
            Categoria c = dao.buscarPorId(id);
            if (c == null) {
                Http.error(resp, 404, "Categoría no encontrada");
            } else {
                Http.enviar(resp, 200, c);
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Categoria c = Http.MAPPER.readValue(req.getInputStream(), Categoria.class);
            if (c.getNombre() == null || c.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Http.enviar(resp, 201, dao.crear(c));
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
            Categoria c = Http.MAPPER.readValue(req.getInputStream(), Categoria.class);
            if (c.getNombre() == null || c.getNombre().isBlank()) {
                Http.error(resp, 400, "El nombre es obligatorio");
                return;
            }
            Categoria actualizada = dao.actualizar(id, c);
            if (actualizada == null) {
                Http.error(resp, 404, "Categoría no encontrada");
            } else {
                Http.enviar(resp, 200, actualizada);
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
            Categoria actualizada = dao.actualizarParcial(id, campos);
            if (actualizada == null) {
                Http.error(resp, 404, "Categoría no encontrada");
            } else {
                Http.enviar(resp, 200, actualizada);
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
                Http.mensaje(resp, 200, "Categoría eliminada");
            } else {
                Http.error(resp, 404, "Categoría no encontrada");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}