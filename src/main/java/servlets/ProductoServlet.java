package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.ProductoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import modelos.Producto;
import util.Http;

@WebServlet("/api/productos/*")
public class ProductoServlet extends HttpServlet {

    private final ProductoDAO dao = new ProductoDAO();

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

    // Valida y completa valores por defecto. Devuelve false si respondió un error.
    private boolean validar(Producto p, HttpServletResponse resp) throws IOException {
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            Http.error(resp, 400, "El nombre es obligatorio");
            return false;
        }
        if (p.getPrecio() == null || p.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            Http.error(resp, 400, "El precio es obligatorio y no puede ser negativo");
            return false;
        }
        if (p.getIdCategoria() == null) {
            Http.error(resp, 400, "La categoría (idCategoria) es obligatoria");
            return false;
        }
        if (p.getStock() == null) {
            p.setStock(0);
        }
        if (p.getStockMinimo() == null) {
            p.setStockMinimo(5);
        }
        if (p.getStock() < 0 || p.getStockMinimo() < 0) {
            Http.error(resp, 400, "El stock no puede ser negativo");
            return false;
        }
        return true;
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
            Producto p = dao.buscarPorId(id);
            if (p == null) {
                Http.error(resp, 404, "Producto no encontrado");
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
            Producto p = Http.MAPPER.readValue(req.getInputStream(), Producto.class);
            if (!validar(p, resp)) {
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
            Producto p = Http.MAPPER.readValue(req.getInputStream(), Producto.class);
            if (!validar(p, resp)) {
                return;
            }
            Producto actualizado = dao.actualizar(id, p);
            if (actualizado == null) {
                Http.error(resp, 404, "Producto no encontrado");
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
            Producto actualizado = dao.actualizarParcial(id, campos);
            if (actualizado == null) {
                Http.error(resp, 404, "Producto no encontrado");
            } else {
                Http.enviar(resp, 200, actualizado);
            }
        } catch (JsonProcessingException | NumberFormatException e) {
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
                Http.mensaje(resp, 200, "Producto eliminado");
            } else {
                Http.error(resp, 404, "Producto no encontrado");
            }
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}