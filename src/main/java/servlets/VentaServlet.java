package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import dao.VentaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import modelos.DetalleVenta;
import modelos.Venta;
import util.ErrorNegocio;
import util.Http;

@WebServlet("/api/ventas/*")
public class VentaServlet extends HttpServlet {

    private static final Set<String> METODOS = Set.of("Efectivo", "Tarjeta", "Transferencia");

    private final VentaDAO dao = new VentaDAO();

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
                Http.enviar(resp, 200, dao.listar(req.getParameter("q")));
                return;
            }
            Integer id = leerId(req, resp);
            if (id == null) {
                return;
            }
            Venta v = dao.buscarPorId(id);
            if (v == null) {
                Http.error(resp, 404, "Venta no encontrada");
            } else {
                Http.enviar(resp, 200, v);
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Venta v = Http.MAPPER.readValue(req.getInputStream(), Venta.class);

            if (v.getMetodoTransaccion() == null || !METODOS.contains(v.getMetodoTransaccion())) {
                Http.error(resp, 400, "metodoTransaccion debe ser Efectivo, Tarjeta o Transferencia");
                return;
            }
            if (v.getDetalles() == null || v.getDetalles().isEmpty()) {
                Http.error(resp, 400, "La venta debe tener al menos un producto");
                return;
            }
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getIdProducto() == null || d.getCantidad() == null || d.getCantidad() <= 0) {
                    Http.error(resp, 400, "Cada producto necesita idProducto y una cantidad mayor a 0");
                    return;
                }
            }

            // El usuario viene en el JSON; si no, se toma de la sesión del login
            if (v.getIdUsuario() == null) {
                HttpSession sesion = req.getSession(false);
                if (sesion != null && sesion.getAttribute("idUsuario") != null) {
                    v.setIdUsuario((Integer) sesion.getAttribute("idUsuario"));
                }
            }
            if (v.getIdUsuario() == null) {
                Http.error(resp, 400, "Falta idUsuario (quién registra la venta)");
                return;
            }

            Http.enviar(resp, 201, dao.crear(v));
        } catch (ErrorNegocio e) {
            Http.error(resp, e.getStatus(), e.getMessage());
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
                Http.mensaje(resp, 200, "Venta anulada y stock devuelto");
            } else {
                Http.error(resp, 404, "Venta no encontrada");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}