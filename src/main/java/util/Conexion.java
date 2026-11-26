package util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Conexion {

    private static DataSource ds;

    public static Connection obtener() throws SQLException {
        try {
            if (ds == null) {
                ds = (DataSource) new InitialContext().lookup("jdbc/postgres");
            }
            return ds.getConnection();
        } catch (NamingException e) {
            throw new SQLException("No se encontró el recurso jdbc/postgres", e);
        }
    }
}