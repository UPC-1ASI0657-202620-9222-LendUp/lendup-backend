package com.lendup.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.web.server.ResponseStatusException;

public final class Asserts {
  private Asserts() {}

  /** Verifica que la acción lance ResponseStatusException con el código HTTP indicado. */
  public static void status(int expected, ThrowingCallable action) {
    assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
        e -> assertThat(e.getStatusCode().value()).isEqualTo(expected));
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> map(Object o) { return (Map<String, Object>) o; }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> list(Object o) { return (List<Map<String, Object>>) o; }
}
