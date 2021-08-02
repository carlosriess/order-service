package com.tutuman.orderservice.dto;

import com.tutuman.orderservice.model.OrderLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDto {
  private List<OrderLine> orderLines;
}
