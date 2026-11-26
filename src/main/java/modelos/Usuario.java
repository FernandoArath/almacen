package modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Usuario {

    private Integer idUsuario;
    private String nombre;
    private String apellidos;
    private String usuario;
    private String status;
    private Integer idRol;
    private String rol; // nombre del rol (el "cargo"), solo lectura

    // Solo entra por JSON, nunca sale en las respuestas
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    // Nunca sale en las respuestas
    @JsonIgnore
    private String contrasenaHash;

    public Usuario() {
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
}