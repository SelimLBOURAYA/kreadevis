package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.mapper.UserMapper;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Override
    public UserResponse findById(Long id) {
        return userMapper.toResponse(userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id)));
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
