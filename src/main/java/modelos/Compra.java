package modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Compra {

    private Integer idCompra;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String metodoTransaccion;
    private Integer idProveedor;
    private String proveedor; // nombre del proveedor, solo lectura
    private List<DetalleCompra> detalles = new ArrayList<>();

    public Compra() {
    }

    public Integer getIdCompra() { return idCompra; }
    public void setIdCompra(Integer idCompra) { this.idCompra = idCompra; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoTransaccion() { return metodoTransaccion; }
    public void setMetodoTransaccion(String metodoTransaccion) { this.metodoTransaccion = metodoTransaccion; }

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public List<DetalleCompra> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCompra> detalles) { this.detalles = detalles; }
}