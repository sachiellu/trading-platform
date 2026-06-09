package com.trading.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "商品名稱不能為空")
    private String name;

    @NotNull(message = "價格不能為空")
    @DecimalMin(value = "0.01", message = "價格必須大於等於 0.01")
    private BigDecimal price;

    @NotNull(message = "庫存數量不能為空")
    @Min(value = 0, message = "庫存數量不能小於 0")
    private Integer stock;
}
