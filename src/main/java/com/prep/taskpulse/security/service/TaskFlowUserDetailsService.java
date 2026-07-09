package com.prep.taskpulse.security.service;

import com.prep.taskpulse.domain.user.repository.UserRepository;
import com.prep.taskpulse.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class TaskFlowUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(TaskFlowUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public TaskFlowUserDetails loadUserById(UUID id){
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .map(TaskFlowUserDetails::new)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
