package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import dao.PermisoRolDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.PermisoRol;
import util.Http;

@WebServlet("/api/permisos-rol/*")
public class PermisoRolServlet extends HttpServlet {

    private final PermisoRolDAO dao = new PermisoRolDAO();

    // GET /api/permisos-rol            -> todas las asignaciones
    // GET /api/permisos-rol?idRol=1    -> permisos de un rol
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String texto = req.getParameter("idRol");
            Integer idRol = null;
            if (texto != null && !texto.isBlank()) {
                try {
                    idRol = Integer.parseInt(texto.trim());
                } catch (NumberFormatException e) {
                    Http.error(resp, 400, "idRol debe ser un número");
                    return;
                }
            }
            Http.enviar(resp, 200, dao.listar(idRol));
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    // POST /api/permisos-rol  con {"idPermiso":1,"idRol":2}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            PermisoRol x = Http.MAPPER.readValue(req.getInputStream(), PermisoRol.class);
            if (x.getIdPermiso() == null || x.getIdRol() == null) {
                Http.error(resp, 400, "idPermiso e idRol son obligatorios");
                return;
            }
            Http.enviar(resp, 201, dao.crear(x));
        } catch (JsonProcessingException e) {
            Http.error(resp, 400, "JSON inválido");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    // DELETE /api/permisos-rol/{idRol}/{idPermiso}
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] ids = Http.ids(req);
        if (ids.length < 2) {
            Http.error(resp, 400, "Usa /api/permisos-rol/{idRol}/{idPermiso}");
            return;
        }
        try {
            int idRol = Integer.parseInt(ids[0]);
            int idPermiso = Integer.parseInt(ids[1]);
            if (dao.eliminar(idRol, idPermiso)) {
                Http.mensaje(resp, 200, "Permiso quitado del rol");
            } else {
                Http.error(resp, 404, "Esa asignación no existe");
            }
        } catch (NumberFormatException e) {
            Http.error(resp, 400, "Los ids deben ser números");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}