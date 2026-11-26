package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import modelos.DetalleVenta;
import modelos.Venta;
import util.Conexion;
import util.ErrorNegocio;

public class VentaDAO {

    private static final String SELECT
            = "SELECT v.id_venta, v.fecha, v.total, v.metodo_transaccion, v.efectivo_recibido, "
            + "v.cambio, v.id_usuario, u.nombre || ' ' || u.apellidos AS usuario "
            + "FROM venta v JOIN usuario u ON u.id_usuario = v.id_usuario";

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("id_venta"));
        v.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        v.setTotal(rs.getBigDecimal("total"));
        v.setMetodoTransaccion(rs.getString("metodo_transaccion"));
        v.setEfectivoRecibido(rs.getBigDecimal("efectivo_recibido"));
        v.setCambio(rs.getBigDecimal("cambio"));
        v.setIdUsuario(rs.getInt("id_usuario"));
        v.setUsuario(rs.getString("usuario"));
        return v;
    }

    // Historial (pantalla Compras). Si viene texto, busca por ID, fecha o monto
    public List<Venta> listar(String texto) throws SQLException {
        boolean busca = texto != null && !texto.isBlank();
        String sql = SELECT
                + (busca ? " WHERE v.id_venta::text ILIKE ? OR v.fecha::text ILIKE ? OR v.total::text ILIKE ?" : "")
                + " ORDER BY v.fecha DESC, v.id_venta DESC";
        List<Venta> lista = new ArrayList<>();
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

    // Una venta con todos sus productos
    public Venta buscarPorId(int id) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            Venta v;
            try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE v.id_venta = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    v = mapear(rs);
                }
            }
            String sql = "SELECT d.id_detalle_venta, d.id_venta, d.id_producto, p.nombre AS producto, "
                    + "d.cantidad, d.precio_unitario, d.subtotal "
                    + "FROM detalle_venta d JOIN producto p ON p.id_producto = d.id_producto "
                    + "WHERE d.id_venta = ? ORDER BY d.id_detalle_venta";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DetalleVenta d = new DetalleVenta();
                        d.setIdDetalleVenta(rs.getInt("id_detalle_venta"));
                        d.setIdVenta(rs.getInt("id_venta"));
                        d.setIdProducto(rs.getInt("id_producto"));
                        d.setProducto(rs.getString("producto"));
                        d.setCantidad(rs.getInt("cantidad"));
                        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                        d.setSubtotal(rs.getBigDecimal("subtotal"));
                        v.getDetalles().add(d);
                    }
                }
            }
            return v;
        }
    }

    // Finalizar venta: todo en una transacción
    public Venta crear(Venta v) throws SQLException, ErrorNegocio {
        int idVenta;
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                List<DetalleVenta> detalles = new ArrayList<>(v.getDetalles());
                detalles.sort(Comparator.comparing(DetalleVenta::getIdProducto));

                BigDecimal total = BigDecimal.ZERO;

                // 1) Bloquear cada producto, revisar stock, descontarlo y tomar su precio real
                for (DetalleVenta d : detalles) {
                    String nombre;
                    BigDecimal precio;
                    int stock;
                    String sqlProducto = "SELECT nombre, precio, stock FROM producto WHERE id_producto = ? FOR UPDATE";
                    try (PreparedStatement ps = con.prepareStatement(sqlProducto)) {
                        ps.setInt(1, d.getIdProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new ErrorNegocio(404, "Producto " + d.getIdProducto() + " no encontrado");
                            }
                            nombre = rs.getString("nombre");
                            precio = rs.getBigDecimal("precio");
                            stock = rs.getInt("stock");
                        }
                    }
                    if (stock < d.getCantidad()) {
                        throw new ErrorNegocio(409, "Stock insuficiente de " + nombre + " (disponible: " + stock + ")");
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE producto SET stock = stock - ? WHERE id_producto = ?")) {
                        ps.setInt(1, d.getCantidad());
                        ps.setInt(2, d.getIdProducto());
                        ps.executeUpdate();
                    }
                    d.setPrecioUnitario(precio);
                    total = total.add(precio.multiply(BigDecimal.valueOf(d.getCantidad())));
                }

                // 2) Pago: la regla de la base exige estos valores según el método
                BigDecimal recibido = null;
                BigDecimal cambio = null;
                if ("Efectivo".equals(v.getMetodoTransaccion())) {
                    if (v.getEfectivoRecibido() == null || v.getEfectivoRecibido().compareTo(total) < 0) {
                        throw new ErrorNegocio(400, "El efectivo recibido debe ser mayor o igual al total ($" + total + ")");
                    }
                    recibido = v.getEfectivoRecibido();
                    cambio = recibido.subtract(total);
                }

                // 3) Guardar la venta
                String sqlVenta = "INSERT INTO venta (total, metodo_transaccion, efectivo_recibido, cambio, id_usuario) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id_venta";
                try (PreparedStatement ps = con.prepareStatement(sqlVenta)) {
                    ps.setBigDecimal(1, total);
                    ps.setString(2, v.getMetodoTransaccion());
                    ps.setBigDecimal(3, recibido);
                    ps.setBigDecimal(4, cambio);
                    ps.setInt(5, v.getIdUsuario());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idVenta = rs.getInt(1);
                    }
                }

                // 4) Guardar los productos de la venta (subtotal lo calcula la base)
                String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                    for (DetalleVenta d : detalles) {
                        ps.setInt(1, idVenta);
                        ps.setInt(2, d.getIdProducto());
                        ps.setInt(3, d.getCantidad());
                        ps.setBigDecimal(4, d.getPrecioUnitario());
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
        return buscarPorId(idVenta);
    }

    // Anular venta: devuelve el stock y borra la venta (sus detalles se borran en cascada)
    public boolean eliminar(int id) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                String sqlStock = "UPDATE producto p SET stock = p.stock + d.cantidad_total "
                        + "FROM (SELECT id_producto, SUM(cantidad) AS cantidad_total "
                        + "FROM detalle_venta WHERE id_venta = ? GROUP BY id_producto) d "
                        + "WHERE p.id_producto = d.id_producto";
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                boolean borrada;
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM venta WHERE id_venta = ?")) {
                    ps.setInt(1, id);
                    borrada = ps.executeUpdate() > 0;
                }
                con.commit();
                return borrada;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}