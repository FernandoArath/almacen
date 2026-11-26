package util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Conexion {

    private static DataSource ds;

    // Lee una variable de entorno o, si no existe, una propiedad de Java (-DNOMBRE=valor)
    private static String leer(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(nombre);
        }
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static synchronized DataSource crear() throws SQLException {
        if (ds != null) {
            return ds;
        }

        String url = convertirUrl(leer("DB_URL"));
        if (url == null) {
            // Render expone PostgreSQL como URL estándar.
            url = convertirUrl(leer("DATABASE_URL"));
        }
        if (url != null) {
            // Nube (Render) o pruebas con una base remota: pool propio con Hikari
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName("PoolAlmacen");
            cfg.setDriverClassName("org.postgresql.Driver");
            cfg.setJdbcUrl(url);
            String usuario = leer("DB_USER");
            String clave = leer("DB_PASSWORD");
            if (usuario != null) {
                cfg.setUsername(usuario);
            }
            if (clave != null) {
                cfg.setPassword(clave);
            }

            String tamano = leer("DB_POOL_SIZE");
            cfg.setMaximumPoolSize(tamano != null ? Integer.parseInt(tamano) : 5);
            cfg.setMinimumIdle(1);
            cfg.setConnectionTimeout(30000);   // espera máxima por una conexión (ms)
            cfg.setIdleTimeout(300000);        // cierra las que llevan 5 min sin usarse
            cfg.setMaxLifetime(1200000);       // renueva cada 20 min
            ds = new HikariDataSource(cfg);
        } else {
            // Local: el pool de GlassFish (recurso jdbc/postgres)
            try {
                ds = (DataSource) new InitialContext().lookup("jdbc/postgres");
            } catch (NamingException e) {
                throw new SQLException("No hay DB_URL ni recurso jdbc/postgres", e);
            }
        }
        return ds;
    }

    private static String convertirUrl(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        return url;
    }

    public static Connection obtener() throws SQLException {
        return crear().getConnection();
    }

    // Cierra el pool de Hikari al apagar o redesplegar la app
    public static synchronized void cerrar() {
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }
        ds = null;
    }
}
