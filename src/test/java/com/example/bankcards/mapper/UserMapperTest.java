package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapperImpl();
    }

    @Test
    void toResponse_nullUser_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_withRoles_mapsAllFields() {
        Role role = Role.builder().name(RoleType.USER).build();
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .roles(new HashSet<>(Set.of(role)))
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
    }

    @Test
    void toResponse_emptyRoles_returnsEmptySet() {
        User user = User.builder()
                .id(2L)
                .username("bob")
                .email("bob@test.com")
                .roles(new HashSet<>())
                .build();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.roles()).isEmpty();
    }

    @Test
    void toResponse_nullRoles_returnsNullRoles() {
        User user = User.builder()
                .id(3L)
                .username("carol")
                .email("carol@test.com")
                .build();
        user.setRoles(null);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.roles()).isNull();
    }
}
