package modelos;

import java.math.BigDecimal;

public class Producto {

    private Integer idProducto;
    private String nombre;
    private String codigoBarras;
    private String imagen;
    private BigDecimal precio;
    private BigDecimal costoCompra;
    private Integer stock;
    private Integer stockMinimo;
    private Integer idCategoria;
    private String categoria; // nombre de la categoría, solo lectura

    public Producto() {
    }

    // Calculado: no es una columna
    public String getEstado() {
        if (stock == null || stock <= 0) {
            return "Sin stock";
        }
        if (stockMinimo != null && stock <= stockMinimo) {
            return "Stock bajo";
        }
        return "Disponible";
    }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getCostoCompra() { return costoCompra; }
    public void setCostoCompra(BigDecimal costoCompra) { this.costoCompra = costoCompra; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}