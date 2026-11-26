package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.Usuario;
import util.Http;

@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO dao = new UsuarioDAO();

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

    private boolean vacio(String s) {
        return s == null || s.isBlank();
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
            Usuario u = dao.buscarPorId(id);
            if (u == null) {
                Http.error(resp, 404, "Usuario no encontrado");
            } else {
                Http.enviar(resp, 200, u);
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Usuario u = Http.MAPPER.readValue(req.getInputStream(), Usuario.class);
            if (vacio(u.getNombre()) || vacio(u.getApellidos()) || vacio(u.getUsuario())
                    || vacio(u.getContrasena()) || u.getIdRol() == null) {
                Http.error(resp, 400, "Nombre, apellidos, usuario, contrasena e idRol son obligatorios");
                return;
            }
            if (vacio(u.getStatus())) {
                u.setStatus("activo");
            }
            Http.enviar(resp, 201, dao.crear(u));
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
            Usuario u = Http.MAPPER.readValue(req.getInputStream(), Usuario.class);
            if (vacio(u.getNombre()) || vacio(u.getApellidos()) || vacio(u.getUsuario())
                    || vacio(u.getStatus()) || u.getIdRol() == null) {
                Http.error(resp, 400, "Nombre, apellidos, usuario, status e idRol son obligatorios");
                return;
            }
            Usuario actualizado = dao.actualizar(id, u);
            if (actualizado == null) {
                Http.error(resp, 404, "Usuario no encontrado");
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
            Usuario actualizado = dao.actualizarParcial(id, campos);
            if (actualizado == null) {
                Http.error(resp, 404, "Usuario no encontrado");
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
                Http.mensaje(resp, 200, "Usuario eliminado");
            } else {
                Http.error(resp, 404, "Usuario no encontrado");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}