package dao;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.Usuario;
import util.Clave;
import util.Conexion;

public class UsuarioDAO {

    private static final String SELECT
            = "SELECT u.id_usuario, u.nombre, u.apellidos, u.usuario, u.status, "
            + "u.id_rol, r.nombre AS rol, u.contrasena_hash "
            + "FROM usuario u JOIN rol r ON r.id = u.id_rol";

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setApellidos(rs.getString("apellidos"));
        u.setUsuario(rs.getString("usuario"));
        u.setStatus(rs.getString("status"));
        u.setIdRol(rs.getInt("id_rol"));
        u.setRol(rs.getString("rol"));
        u.setContrasenaHash(rs.getString("contrasena_hash"));
        return u;
    }

    public List<Usuario> listar() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY u.id_usuario");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Usuario buscarPorId(int id) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE u.id_usuario = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // Para el login
    public Usuario buscarPorUsuario(String usuario) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE u.usuario = ?")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Usuario crear(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, apellidos, usuario, contrasena_hash, status, id_rol) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_usuario";
        int nuevoId;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellidos());
            ps.setString(3, u.getUsuario());
            ps.setString(4, Clave.hashear(u.getContrasena()));
            ps.setString(5, u.getStatus());
            ps.setInt(6, u.getIdRol());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                nuevoId = rs.getInt(1);
            }
        }
        return buscarPorId(nuevoId);
    }

    // PUT: si no mandas contrasena, se conserva la actual
    public Usuario actualizar(int id, Usuario u) throws SQLException {
        boolean cambiaClave = u.getContrasena() != null && !u.getContrasena().isBlank();
        String sql = "UPDATE usuario SET nombre = ?, apellidos = ?, usuario = ?, status = ?, id_rol = ?"
                + (cambiaClave ? ", contrasena_hash = ?" : "")
                + " WHERE id_usuario = ?";
        int filas;
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getNombre());
            ps.setString(i++, u.getApellidos());
            ps.setString(i++, u.getUsuario());
            ps.setString(i++, u.getStatus());
            ps.setInt(i++, u.getIdRol());
            if (cambiaClave) {
                ps.setString(i++, Clave.hashear(u.getContrasena()));
            }
            ps.setInt(i, id);
            filas = ps.executeUpdate();
        }
        return filas == 0 ? null : buscarPorId(id);
    }

    // PATCH: solo cambia lo que venga en el JSON (sirve para suspender/reactivar)
    public Usuario actualizarParcial(int id, JsonNode campos) throws SQLException {
        Usuario actual = buscarPorId(id);
        if (actual == null) {
            return null;
        }
        if (campos.hasNonNull("nombre")) {
            actual.setNombre(campos.get("nombre").asText());
        }
        if (campos.hasNonNull("apellidos")) {
            actual.setApellidos(campos.get("apellidos").asText());
        }
        if (campos.hasNonNull("usuario")) {
            actual.setUsuario(campos.get("usuario").asText());
        }
        if (campos.hasNonNull("status")) {
            actual.setStatus(campos.get("status").asText());
        }
        if (campos.hasNonNull("idRol")) {
            actual.setIdRol(campos.get("idRol").asInt());
        }
        if (campos.hasNonNull("contrasena")) {
            actual.setContrasena(campos.get("contrasena").asText());
        }
        return actualizar(id, actual);
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}