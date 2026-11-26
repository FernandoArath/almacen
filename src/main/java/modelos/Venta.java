package modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venta {

    private Integer idVenta;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String metodoTransaccion;
    private BigDecimal efectivoRecibido;
    private BigDecimal cambio;
    private Integer idUsuario;
    private String usuario; // nombre de quien vendió, solo lectura
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
    }

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoTransaccion() { return metodoTransaccion; }
    public void setMetodoTransaccion(String metodoTransaccion) { this.metodoTransaccion = metodoTransaccion; }

    public BigDecimal getEfectivoRecibido() { return efectivoRecibido; }
    public void setEfectivoRecibido(BigDecimal efectivoRecibido) { this.efectivoRecibido = efectivoRecibido; }

    public BigDecimal getCambio() { return cambio; }
    public void setCambio(BigDecimal cambio) { this.cambio = cambio; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}