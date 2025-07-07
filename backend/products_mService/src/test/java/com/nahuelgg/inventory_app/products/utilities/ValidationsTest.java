package com.nahuelgg.inventory_app.products.utilities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.nahuelgg.inventory_app.products.utilities.Validations.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nahuelgg.inventory_app.products.exceptions.EmptyFieldException;

public class ValidationsTest {
  @Test
  void checkFieldHasContent() {
    assertDoesNotThrow(() -> checkFieldsHasContent(new Field("name", "value")));
    assertDoesNotThrow(() -> checkFieldsHasContent(
      new Field("name", "value"),
      new Field("name", List.of("asd", "asd"))
    ));
  }

  @Test
  void checkFieldHasContent_throwEmptyFieldException() {
    Field successValue = new Field("name", "value");
    Field nullValue = new Field("name", null);
    Field emptyStringValue = new Field("name", "");
    Field emptyListValue = new Field("name", Arrays.asList());
    Field nullValueInListValue = new Field("name", Arrays.asList("asd", null));
    Field emptyValueInListValue = new Field("name", Arrays.asList("asd", ""));

    assertAll(
      () -> assertThrows(EmptyFieldException.class, () -> checkFieldsHasContent(successValue, nullValue)),
      () -> assertThrows(EmptyFieldException.class, () -> checkFieldsHasContent(successValue, emptyStringValue)),
      () -> assertThrows(EmptyFieldException.class, () -> checkFieldsHasContent(successValue, emptyListValue)),
      () -> assertThrows(EmptyFieldException.class, () -> checkFieldsHasContent(successValue, nullValueInListValue)),
      () -> assertThrows(EmptyFieldException.class, () -> checkFieldsHasContent(successValue, emptyValueInListValue))
    );
  }
}
