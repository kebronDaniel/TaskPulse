package com.prep.taskpulse.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.prep.taskpulse.auth.dto.LoginRequest;
import com.prep.taskpulse.auth.dto.RegisterRequest;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.security.jwt.secret=01234567890123456789012345678901",
        "app.security.jwt.expiration-ms=900000"
})
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoad(){}


    @Test
    void register_returnsCreatedAndToken() throws Exception{

        RegisterRequest request = new RegisterRequest("kebron daniel","keb@gmail.com","password");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        assertThat(userRepository.existsByEmailAndDeletedAtIsNull(request.email())).isTrue();
    }

    @Test
    void register_duplicate_returnsConflict() throws Exception{

        User user = User.createUser("Mark","mark@gmail.com","markpass", Role.USER);
        userRepository.save(user);

        RegisterRequest request = new RegisterRequest("mark","mark@gmail.com","markpass");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));

    }

    @Test
    void login_returnsOkAndToken() throws Exception{

        String hashPassword = passwordEncoder.encode("testmark");
        User user = User.createUser("Mark","mark@gmail.com",hashPassword, Role.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("mark@gmail.com","testmark");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withInvalidPassword_returnsUnauthorized() throws Exception{
        String hashPassword = passwordEncoder.encode("testmark");
        User user = User.createUser("Mark","mark@gmail.com",hashPassword, Role.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("mark@gmail.com","testmar");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void login_withUnknownEmail_returnsUnauthorized() throws Exception{
        String hashPassword = passwordEncoder.encode("testmark");
        User user = User.createUser("Mark","mark@gmail.com",hashPassword, Role.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("mar@gmail.com","testmark");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void register_withInvalidEmail_returnsBadRequest() throws Exception{

        RegisterRequest request = new RegisterRequest("kebron daniel","keb.com","password");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

    }

    @Test
    void protectedEndpoint_withoutJwt_returnsUnauthorized() throws Exception{

        UUID mockWorkspaceUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");
        UUID mockProjectUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174222");
        UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks"
                ,mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidJwt_returnsNotFoundInsteadOfUnauthorized() throws Exception{

        String hashPassword = passwordEncoder.encode("testmark");
        User user = User.createUser("Mark","mark@gmail.com",hashPassword, Role.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("mark@gmail.com","testmark");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String jwt = JsonPath.read(responseBody,"$.accessToken");

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}"
                ,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID())
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
