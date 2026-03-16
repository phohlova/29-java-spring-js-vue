package org.example._9javaspringjsvue.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status; // Используем String для простоты передачи на фронт
    private ZonedDateTime createdAt;
    private List<OrderItemDTO> items;
}