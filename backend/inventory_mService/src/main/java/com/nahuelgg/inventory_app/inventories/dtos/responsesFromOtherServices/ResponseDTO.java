package com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ResponseDTO {
  private Integer status;
  private ErrorDTO error;
  private Object data;
}
