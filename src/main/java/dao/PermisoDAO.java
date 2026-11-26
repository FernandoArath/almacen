package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Permiso;
import util.Conexion;

public class PermisoDAO {

    private static final String COLUMNAS = "id, nombre";

    private Permiso mapear(ResultSet rs) throws SQLException {
        Permiso p = new Permiso();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        return p;
    }

    public List<Permiso> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM permiso ORDER BY id";
        List<Permiso> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Permiso buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM permiso WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Permiso crear(Permiso p) throws SQLException {
        String sql = "INSERT INTO permiso (nombre) VALUES (?) RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    public Permiso actualizar(int id, Permiso p) throws SQLException {
        String sql = "UPDATE permiso SET nombre = ? WHERE id = ? RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Permiso actualizarParcial(int id, JsonNode campos) throws SQLException {
        Permiso actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.has("nombre")) {
            actual.setNombre(campos.get("nombre").isNull() ? null : campos.get("nombre").asText());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM permiso WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}