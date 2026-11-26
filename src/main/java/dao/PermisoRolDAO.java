package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelos.PermisoRol;
import util.Conexion;

public class PermisoRolDAO {

    private static final String SELECT
            = "SELECT pr.id_permiso, pr.id_rol, p.nombre AS permiso, r.nombre AS rol "
            + "FROM permiso_rol pr "
            + "JOIN permiso p ON p.id = pr.id_permiso "
            + "JOIN rol r ON r.id = pr.id_rol";

    private PermisoRol mapear(ResultSet rs) throws SQLException {
        PermisoRol x = new PermisoRol();
        x.setIdPermiso(rs.getInt("id_permiso"));
        x.setIdRol(rs.getInt("id_rol"));
        x.setPermiso(rs.getString("permiso"));
        x.setRol(rs.getString("rol"));
        return x;
    }

    // Si viene idRol, lista solo los permisos de ese rol
    public List<PermisoRol> listar(Integer idRol) throws SQLException {
        String sql = SELECT + (idRol != null ? " WHERE pr.id_rol = ?" : "")
                + " ORDER BY pr.id_rol, pr.id_permiso";
        List<PermisoRol> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (idRol != null) {
                ps.setInt(1, idRol);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public PermisoRol buscar(int idRol, int idPermiso) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     SELECT + " WHERE pr.id_rol = ? AND pr.id_permiso = ?")) {
            ps.setInt(1, idRol);
            ps.setInt(2, idPermiso);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public PermisoRol crear(PermisoRol x) throws SQLException {
        String sql = "INSERT INTO permiso_rol (id_permiso, id_rol) VALUES (?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, x.getIdPermiso());
            ps.setInt(2, x.getIdRol());
            ps.executeUpdate();
        }
        return buscar(x.getIdRol(), x.getIdPermiso());
    }

    public boolean eliminar(int idRol, int idPermiso) throws SQLException {
        String sql = "DELETE FROM permiso_rol WHERE id_rol = ? AND id_permiso = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRol);
            ps.setInt(2, idPermiso);
            return ps.executeUpdate() > 0;
        }
    }
}