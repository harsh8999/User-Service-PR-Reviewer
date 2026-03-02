package com.harsh.user_service.repository;

import com.harsh.user_service.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAll(int page, int size);

    boolean deleteById(Long id);

    boolean existsByEmail(String email);
}
