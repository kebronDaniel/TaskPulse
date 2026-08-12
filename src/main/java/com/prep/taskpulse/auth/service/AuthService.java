package com.prep.taskpulse.auth.service;

import com.prep.taskpulse.auth.dto.AuthResponse;
import com.prep.taskpulse.auth.dto.LoginRequest;
import com.prep.taskpulse.auth.dto.RegisterRequest;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.user.repository.UserRepository;
import com.prep.taskpulse.exception.EmailAlreadyExistsException;
import com.prep.taskpulse.security.jwt.JwtService;
import com.prep.taskpulse.security.service.TaskFlowUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthResponse register(RegisterRequest request) {

    if (userRepository.existsByEmailAndDeletedAtIsNull(request.email()))
      throw new EmailAlreadyExistsException();

    String passwordHash = passwordEncoder.encode(request.password());

    User user = User.createUser(request.fullName(), request.email(), passwordHash, Role.USER);
    User savedUser = userRepository.save(user);

    String jwt = jwtService.generateToken(new TaskFlowUserDetails(savedUser));
    return new AuthResponse(jwt, "Bearer");
  }

  public AuthResponse login(LoginRequest request) {
    // Does all these TaskFlowUserDetailsService, UserRepository, PasswordEncoder.matches(...)
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    TaskFlowUserDetails userDetails = (TaskFlowUserDetails) authentication.getPrincipal();
    String jwt = jwtService.generateToken(userDetails);
    // the client can use this for later stateless requests.
    return new AuthResponse(jwt, "Bearer");
  }
}
