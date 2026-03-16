package org.example._9javaspringjsvue.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryDTO {
    private Integer totalItems;
    private BigDecimal totalAmount;
}
