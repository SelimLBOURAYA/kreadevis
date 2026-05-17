package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "authorities", target = "roles")
    UserResponse toResponse(User user);

    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream().map(r -> r.getName().name()).collect(Collectors.toSet());
    }
}
