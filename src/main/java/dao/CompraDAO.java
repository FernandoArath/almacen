package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import modelos.Compra;
import modelos.DetalleCompra;
import util.Conexion;
import util.ErrorNegocio;
import util.Sql;

public class CompraDAO {

    private static final String SELECT
            = "SELECT c.id_compra, c.fecha, c.total, c.metodo_transaccion, c.id_proveedor, "
            + "pr.nombre AS proveedor "
            + "FROM compra c LEFT JOIN proveedor pr ON pr.id_proveedor = c.id_proveedor";

    private Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setIdCompra(rs.getInt("id_compra"));
        c.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        c.setTotal(rs.getBigDecimal("total"));
        c.setMetodoTransaccion(rs.getString("metodo_transaccion"));
        c.setIdProveedor(Sql.entero(rs, "id_proveedor"));
        c.setProveedor(rs.getString("proveedor"));
        return c;
    }

    public List<Compra> listar(String texto) throws SQLException {
        boolean busca = texto != null && !texto.isBlank();
        String sql = SELECT
                + (busca ? " WHERE c.id_compra::text ILIKE ? OR c.fecha::text ILIKE ? OR c.total::text ILIKE ?" : "")
                + " ORDER BY c.fecha DESC, c.id_compra DESC";
        List<Compra> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (busca) {
                String patron = "%" + texto.trim() + "%";
                ps.setString(1, patron);
                ps.setString(2, patron);
                ps.setString(3, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Compra buscarPorId(int id) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            Compra c;
            try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE c.id_compra = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    c = mapear(rs);
                }
            }
            String sql = "SELECT d.id_detalle_compra, d.id_compra, d.id_producto, p.nombre AS producto, "
                    + "d.cantidad, d.costo_unitario, d.subtotal "
                    + "FROM detalle_compra d JOIN producto p ON p.id_producto = d.id_producto "
                    + "WHERE d.id_compra = ? ORDER BY d.id_detalle_compra";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DetalleCompra d = new DetalleCompra();
                        d.setIdDetalleCompra(rs.getInt("id_detalle_compra"));
                        d.setIdCompra(rs.getInt("id_compra"));
                        d.setIdProducto(rs.getInt("id_producto"));
                        d.setProducto(rs.getString("producto"));
                        d.setCantidad(rs.getInt("cantidad"));
                        d.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                        d.setSubtotal(rs.getBigDecimal("subtotal"));
                        c.getDetalles().add(d);
                    }
                }
            }
            return c;
        }
    }

    // Agregar stock: todo en una transacción
    public Compra crear(Compra c) throws SQLException, ErrorNegocio {
        int idCompra;
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                List<DetalleCompra> detalles = new ArrayList<>(c.getDetalles());
                detalles.sort(Comparator.comparing(DetalleCompra::getIdProducto));

                BigDecimal total = BigDecimal.ZERO;

                // 1) Bloquear cada producto y sumarle el stock
                for (DetalleCompra d : detalles) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT 1 FROM producto WHERE id_producto = ? FOR UPDATE")) {
                        ps.setInt(1, d.getIdProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new ErrorNegocio(404, "Producto " + d.getIdProducto() + " no encontrado");
                            }
                        }
                    }
                    // Si viene un costo mayor a 0, también actualiza el costo de compra del producto
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE producto SET stock = stock + ?, "
                            + "costo_compra = CASE WHEN ? > 0 THEN ? ELSE costo_compra END "
                            + "WHERE id_producto = ?")) {
                        ps.setInt(1, d.getCantidad());
                        ps.setBigDecimal(2, d.getCostoUnitario());
                        ps.setBigDecimal(3, d.getCostoUnitario());
                        ps.setInt(4, d.getIdProducto());
                        ps.executeUpdate();
                    }
                    total = total.add(d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
                }

                // 2) Guardar la compra (id_proveedor y metodo_transaccion son opcionales)
                String sqlCompra = "INSERT INTO compra (total, metodo_transaccion, id_proveedor) "
                        + "VALUES (?, ?, ?) RETURNING id_compra";
                try (PreparedStatement ps = con.prepareStatement(sqlCompra)) {
                    ps.setBigDecimal(1, total);
                    ps.setString(2, c.getMetodoTransaccion());
                    Sql.setEntero(ps, 3, c.getIdProveedor());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idCompra = rs.getInt(1);
                    }
                }

                // 3) Guardar los productos (subtotal lo calcula la base)
                String sqlDetalle = "INSERT INTO detalle_compra (id_compra, id_producto, cantidad, costo_unitario) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                    for (DetalleCompra d : detalles) {
                        ps.setInt(1, idCompra);
                        ps.setInt(2, d.getIdProducto());
                        ps.setInt(3, d.getCantidad());
                        ps.setBigDecimal(4, d.getCostoUnitario());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
        return buscarPorId(idCompra);
    }

    // Anular compra: quita el stock que había sumado (si todavía hay suficiente)
    public boolean eliminar(int id) throws SQLException, ErrorNegocio {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM compra WHERE id_compra = ? FOR UPDATE")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }

                List<int[]> cantidades = new ArrayList<>(); // {idProducto, cantidad}
                String sqlDetalles = "SELECT id_producto, SUM(cantidad) AS cantidad FROM detalle_compra "
                        + "WHERE id_compra = ? GROUP BY id_producto ORDER BY id_producto";
                try (PreparedStatement ps = con.prepareStatement(sqlDetalles)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            cantidades.add(new int[]{rs.getInt("id_producto"), rs.getInt("cantidad")});
                        }
                    }
                }

                for (int[] item : cantidades) {
                    String nombre;
                    int stock;
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT nombre, stock FROM producto WHERE id_producto = ? FOR UPDATE")) {
                        ps.setInt(1, item[0]);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            nombre = rs.getString("nombre");
                            stock = rs.getInt("stock");
                        }
                    }
                    if (stock < item[1]) {
                        throw new ErrorNegocio(409, "No se puede anular: " + nombre
                                + " ya no tiene stock suficiente (stock actual: " + stock + ")");
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE producto SET stock = stock - ? WHERE id_producto = ?")) {
                        ps.setInt(1, item[1]);
                        ps.setInt(2, item[0]);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM compra WHERE id_compra = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}