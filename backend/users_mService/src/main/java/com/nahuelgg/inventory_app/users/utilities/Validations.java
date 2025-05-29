package com.nahuelgg.inventory_app.users.utilities;

import java.lang.reflect.Array;
import java.util.List;

import com.nahuelgg.inventory_app.users.exceptions.EmptyFieldException;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class Validations {
  @Getter @AllArgsConstructor
  public static class Field {
    private String name;
    private Object value;
  }

  public static void checkFieldsHasContent(Field... fields) {
    for (int i = 0; i < fields.length; i++) {
      String name = fields[i].getName();
      Object value = fields[i].getValue();

      if (value == null)
        throw new EmptyFieldException(name);

      if (value instanceof String && ((String) value).isBlank())
        throw new EmptyFieldException(name);

      if (value instanceof List) {
        List<?> list = (List<?>) value;

        if (list.isEmpty()) 
          throw new EmptyFieldException(name);
        
        for (int j = 0; j < list.size(); j++) {
          Object object = list.get(j);
          Integer ji = j + 1;
          checkFieldsHasContent(new Field(String.format("%s' en la posición '%s", name, ji.toString()), object));
        }
      }

      if (value instanceof Array) {
        Object[] array = (Object[]) value;

        if (array.length == 0)
          throw new EmptyFieldException(name);

        for (int j = 0; j < array.length; j++) {
          Object object = array[j];
          Integer ji = j + 1;
          checkFieldsHasContent(new Field(String.format("%s' en la posición '%s", name, ji.toString()), object));
        }
      }
    }
  }
}
