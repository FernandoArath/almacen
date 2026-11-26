package servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dao.UsuarioDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import modelos.Usuario;
import util.Clave;
import util.Http;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private final UsuarioDAO dao = new UsuarioDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonNode datos = Http.MAPPER.readTree(req.getInputStream());
            String usuario = datos.hasNonNull("usuario") ? datos.get("usuario").asText() : "";
            String contrasena = datos.hasNonNull("contrasena") ? datos.get("contrasena").asText() : "";

            if (usuario.isBlank() || contrasena.isBlank()) {
                Http.error(resp, 400, "Usuario y contraseña son obligatorios");
                return;
            }

            Usuario u = dao.buscarPorUsuario(usuario);
            if (u == null || !Clave.verificar(contrasena, u.getContrasenaHash())) {
                Http.error(resp, 401, "Usuario o contraseña incorrectos");
                return;
            }
            if (!"Activo".equalsIgnoreCase(u.getStatus())) {
                Http.error(resp, 403, "Usuario suspendido");
                return;
            }

            req.getSession(true).setAttribute("idUsuario", u.getIdUsuario());
            Http.enviar(resp, 200, u);
        } catch (JsonProcessingException e) {
            Http.error(resp, 400, "JSON inválido");
        } catch (SQLException e) {
            Http.errorSql(resp, e);
        }
    }
}