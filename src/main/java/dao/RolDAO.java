package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Rol;
import util.Conexion;

public class RolDAO {

    private static final String COLUMNAS = "id, nombre";

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol r = new Rol();
        r.setId(rs.getInt("id"));
        r.setNombre(rs.getString("nombre"));
        return r;
    }

    public List<Rol> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM rol ORDER BY id";
        List<Rol> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Rol buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM rol WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Rol crear(Rol r) throws SQLException {
        String sql = "INSERT INTO rol (nombre) VALUES (?) RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    public Rol actualizar(int id, Rol r) throws SQLException {
        String sql = "UPDATE rol SET nombre = ? WHERE id = ? RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Rol actualizarParcial(int id, JsonNode campos) throws SQLException {
        Rol actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.has("nombre")) {
            actual.setNombre(campos.get("nombre").isNull() ? null : campos.get("nombre").asText());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM rol WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}