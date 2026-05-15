package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
