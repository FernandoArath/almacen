package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Categoria;
import util.Conexion;

public class CategoriaDao {

    private static final String COLUMNAS = "id, nombre, descripcion";

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        return c;
    }

    public List<Categoria> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM categoria ORDER BY id";
        List<Categoria> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Categoria buscarPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM categoria WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Categoria crear(Categoria c) throws SQLException {
        String sql = "INSERT INTO categoria (nombre, descripcion) VALUES (?, ?) RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDescripcion());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapear(rs);
            }
        }
    }

    // UPDATE completo (PUT). Devuelve null si el id no existe.
    public Categoria actualizar(int id, Categoria c) throws SQLException {
        String sql = "UPDATE categoria SET nombre = ?, descripcion = ? WHERE id = ? RETURNING " + COLUMNAS;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDescripcion());
            ps.setInt(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // UPDATE parcial (PATCH): solo cambia lo que venga en el JSON
    public Categoria actualizarParcial(int id, JsonNode campos) throws SQLException {
        Categoria actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.has("nombre")) {
            actual.setNombre(campos.get("nombre").isNull() ? null : campos.get("nombre").asText());
        }
        if (campos.has("descripcion")) {
            actual.setDescripcion(campos.get("descripcion").isNull() ? null : campos.get("descripcion").asText());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM categoria WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}