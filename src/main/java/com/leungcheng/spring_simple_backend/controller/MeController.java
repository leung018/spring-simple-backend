package com.leungcheng.spring_simple_backend.controller;

import com.leungcheng.spring_simple_backend.auth.UserAuthenticatedInfoToken;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
  @Autowired private UserRepository userRepository;

  @GetMapping("/me")
  public UserAccountInfo me(UserAuthenticatedInfoToken authToken) {
    String userId = authToken.getPrincipal();
    User user = userRepository.findById(userId).orElseThrow();
    return new UserAccountInfo(user.getUsername(), user.getBalance());
  }

  public record UserAccountInfo(String username, BigDecimal balance) {}
}
