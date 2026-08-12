package com.prep.taskpulse.security.service;

import com.prep.taskpulse.config.CacheConfig;
import com.prep.taskpulse.domain.user.repository.UserRepository;
import com.prep.taskpulse.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class TaskFlowUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository
        .findByEmailAndDeletedAtIsNull(email)
        .map(TaskFlowUserDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Cacheable(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
  public TaskFlowUserDetails loadUserById(UUID id) {
    return userRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(TaskFlowUserDetails::new)
        .orElseThrow(() -> new UserNotFoundException(id));
  }
}
