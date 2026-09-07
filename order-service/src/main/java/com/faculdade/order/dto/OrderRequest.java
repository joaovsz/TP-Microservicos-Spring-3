package com.faculdade.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderRequest {

    @NotBlank(message = "O item do pedido é obrigatório")
    private String item;

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima deve ser 1")
    private Integer quantity;

    @NotNull(message = "O preço total é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço total deve ser maior que zero")
    private Double totalPrice;

    public OrderRequest() {
    }

    public OrderRequest(String item, Integer quantity, Double totalPrice) {
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
