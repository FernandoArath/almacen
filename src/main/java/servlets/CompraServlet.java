package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import dao.CompraDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import modelos.Compra;
import modelos.DetalleCompra;
import util.ErrorNegocio;
import util.Http;

@WebServlet("/api/compras/*")
public class CompraServlet extends HttpServlet {

    private final CompraDAO dao = new CompraDAO();

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
            Compra c = dao.buscarPorId(id);
            if (c == null) {
                Http.error(resp, 404, "Compra no encontrada");
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
            Compra c = Http.MAPPER.readValue(req.getInputStream(), Compra.class);

            if (c.getDetalles() == null || c.getDetalles().isEmpty()) {
                Http.error(resp, 400, "La compra debe tener al menos un producto");
                return;
            }
            for (DetalleCompra d : c.getDetalles()) {
                if (d.getIdProducto() == null || d.getCantidad() == null || d.getCantidad() <= 0) {
                    Http.error(resp, 400, "Cada producto necesita idProducto y una cantidad mayor a 0");
                    return;
                }
                if (d.getCostoUnitario() == null) {
                    d.setCostoUnitario(BigDecimal.ZERO);
                }
                if (d.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0) {
                    Http.error(resp, 400, "El costo no puede ser negativo");
                    return;
                }
            }

            Http.enviar(resp, 201, dao.crear(c));
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
                Http.mensaje(resp, 200, "Compra anulada y stock descontado");
            } else {
                Http.error(resp, 404, "Compra no encontrada");
            }
        } catch (ErrorNegocio e) {
            Http.error(resp, e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}