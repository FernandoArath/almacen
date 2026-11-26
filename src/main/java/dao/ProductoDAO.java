package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Producto;
import util.Conexion;

public class ProductoDAO {

    private static final String SELECT
            = "SELECT p.id_producto, p.nombre, p.codigo_barras, p.imagen, p.precio, p.costo_compra, "
            + "p.stock, p.stock_minimo, p.id_categoria, c.nombre AS categoria "
            + "FROM producto p JOIN categoria c ON c.id = p.id_categoria";

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setImagen(rs.getString("imagen"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setCostoCompra(rs.getBigDecimal("costo_compra"));
        p.setStock(rs.getInt("stock"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setCategoria(rs.getString("categoria"));
        return p;
    }

    // Si viene texto, busca por nombre o código de barras
    public List<Producto> listar(String texto) throws SQLException {
        boolean busca = texto != null && !texto.isBlank();
        String sql = SELECT
                + (busca ? " WHERE p.nombre ILIKE ? OR p.codigo_barras ILIKE ?" : "")
                + " ORDER BY p.id_producto";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (busca) {
                String patron = "%" + texto.trim() + "%";
                ps.setString(1, patron);
                ps.setString(2, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Producto buscarPorId(int id) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE p.id_producto = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Producto crear(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (nombre, codigo_barras, imagen, precio, costo_compra, "
                + "stock, stock_minimo, id_categoria) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_producto";
        int nuevoId;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getImagen());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setBigDecimal(5, p.getCostoCompra());
            ps.setInt(6, p.getStock());
            ps.setInt(7, p.getStockMinimo());
            ps.setInt(8, p.getIdCategoria());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                nuevoId = rs.getInt(1);
            }
        }
        return buscarPorId(nuevoId);
    }

    public Producto actualizar(int id, Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, codigo_barras = ?, imagen = ?, precio = ?, "
                + "costo_compra = ?, stock = ?, stock_minimo = ?, id_categoria = ? WHERE id_producto = ?";
        int filas;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getImagen());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setBigDecimal(5, p.getCostoCompra());
            ps.setInt(6, p.getStock());
            ps.setInt(7, p.getStockMinimo());
            ps.setInt(8, p.getIdCategoria());
            ps.setInt(9, id);
            filas = ps.executeUpdate();
        }
        return filas == 0 ? null : buscarPorId(id);
    }

    // PATCH: solo cambia lo que venga en el JSON
    public Producto actualizarParcial(int id, JsonNode campos) throws SQLException {
        Producto actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.hasNonNull("nombre")) {
            actual.setNombre(campos.get("nombre").asText());
        }
        if (campos.has("codigoBarras")) {
            actual.setCodigoBarras(campos.get("codigoBarras").isNull() ? null : campos.get("codigoBarras").asText());
        }
        if (campos.has("imagen")) {
            actual.setImagen(campos.get("imagen").isNull() ? null : campos.get("imagen").asText());
        }
        if (campos.hasNonNull("precio")) {
            actual.setPrecio(new BigDecimal(campos.get("precio").asText()));
        }
        if (campos.has("costoCompra")) {
            actual.setCostoCompra(campos.get("costoCompra").isNull() ? null : new BigDecimal(campos.get("costoCompra").asText()));
        }
        if (campos.hasNonNull("stock")) {
            actual.setStock(campos.get("stock").asInt());
        }
        if (campos.hasNonNull("stockMinimo")) {
            actual.setStockMinimo(campos.get("stockMinimo").asInt());
        }
        if (campos.hasNonNull("idCategoria")) {
            actual.setIdCategoria(campos.get("idCategoria").asInt());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}