package modelos;

public class PermisoRol {

    private Integer idPermiso;
    private Integer idRol;
    private String permiso; // nombre del permiso, solo lectura
    private String rol;     // nombre del rol, solo lectura

    public PermisoRol() {
    }

    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getPermiso() { return permiso; }
    public void setPermiso(String permiso) { this.permiso = permiso; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}