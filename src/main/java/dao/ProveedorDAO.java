package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Proveedor;
import util.Conexion;

public class ProveedorDAO {

    private static final String COLUMNAS = "id_proveedor, nombre, contacto, telefono";

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setNombre(rs.getString("nombre"));
        p.setContacto(rs.getString("contacto"));
        p.setTelefono(rs.getString("telefono"));
        return p;
    }

    public List<Proveedor> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM proveedor ORDER BY id_proveedor";
        List<Proveedor> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM proveedor WHERE id_proveedor = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Proveedor crear(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedor (nombre, contacto, telefono) VALUES (?, ?, ?) RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getContacto());
            ps.setString(3, p.getTelefono());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    public Proveedor actualizar(int id, Proveedor p) throws SQLException {
        String sql = "UPDATE proveedor SET nombre = ?, contacto = ?, telefono = ? WHERE id_proveedor = ? RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getContacto());
            ps.setString(3, p.getTelefono());
            ps.setInt(4, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Proveedor actualizarParcial(int id, JsonNode campos) throws SQLException {
        Proveedor actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.has("nombre")) {
            actual.setNombre(campos.get("nombre").isNull() ? null : campos.get("nombre").asText());
        }
        if (campos.has("contacto")) {
            actual.setContacto(campos.get("contacto").isNull() ? null : campos.get("contacto").asText());
        }
        if (campos.has("telefono")) {
            actual.setTelefono(campos.get("telefono").isNull() ? null : campos.get("telefono").asText());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM proveedor WHERE id_proveedor = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}