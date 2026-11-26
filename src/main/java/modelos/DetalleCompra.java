package modelos;

import java.math.BigDecimal;

public class DetalleCompra {

    private Integer idDetalleCompra;
    private Integer idCompra;
    private Integer idProducto;
    private String producto; // nombre del producto, solo lectura
    private Integer cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;

    public DetalleCompra() {
    }

    public Integer getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(Integer idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }

    public Integer getIdCompra() { return idCompra; }
    public void setIdCompra(Integer idCompra) { this.idCompra = idCompra; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}