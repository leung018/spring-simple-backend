package com.leungcheng.spring_simple_backend.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.leungcheng.spring_simple_backend.validation.ObjectValidator;
import org.junit.jupiter.api.Test;

class UserTest {
  private static User.Builder userBuilder() {
    return new User.Builder().username("default_user").password("default_password").balance(1.0);
  }

  @Test
  void shouldCreateUser() {
    User user = userBuilder().username("user_1").password("password").balance(25).build();

    assertEquals("user_1", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals(25, user.getBalance());
  }

  @Test
  void shouldIdIsDifferentForEachBuild() {
    User user1 = userBuilder().build();
    User user2 = userBuilder().build();

    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  void shouldRaiseExceptionWhenBuild_IfParamsViolateTheValidationConstraints() {
    assertThrowValidationException(userBuilder().username(null));
    assertThrowValidationException(userBuilder().username(""));

    assertThrowValidationException(userBuilder().password(null));
    assertThrowValidationException(userBuilder().password(""));
  }

  private void assertThrowValidationException(User.Builder builder) {
    Class<ObjectValidator.ObjectValidationException> expected =
        ObjectValidator.ObjectValidationException.class;
    assertThrows(expected, builder::build);
  }
}
