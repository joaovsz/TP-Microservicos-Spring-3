package com.faculdade.order.dto;

public record OrderResponse(
        Long id,
        String customer,
        String customerRole,
        String item,
        Integer quantity,
        Double totalPrice,
        String status
) {
    public static OrderResponse of(com.faculdade.order.model.Order order, String role) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer(),
                role != null ? role : "ROLE_USER",
                order.getItem(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}
