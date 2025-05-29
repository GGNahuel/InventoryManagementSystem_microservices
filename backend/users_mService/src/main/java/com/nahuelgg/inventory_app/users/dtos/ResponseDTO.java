package com.nahuelgg.inventory_app.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ResponseDTO {
  private Integer status;
  private ErrorDTO error;
  private Object data;
}
