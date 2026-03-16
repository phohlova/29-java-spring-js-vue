package org.example._9javaspringjsvue.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long productId;
    private String productTitle;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}