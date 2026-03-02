package com.harsh.user_service.service;

import com.harsh.user_service.dto.CreateUserRequest;
import com.harsh.user_service.dto.UpdateUserRequest;
import com.harsh.user_service.exception.UserNotFoundException;
import com.harsh.user_service.model.User;
import com.harsh.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(CreateUserRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        // Email uniqueness is enforced atomically inside the repository
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<User> getAllUsers(int page, int size) {
        return userRepository.findAll(page, size);
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = getUserById(id);
        // Build a new object rather than mutating the stored reference directly
        User updated = User.builder()
                .id(existing.getId())
                .firstName(request.getFirstName() != null ? request.getFirstName() : existing.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : existing.getLastName())
                .email(existing.getEmail())
                .phone(request.getPhone() != null ? request.getPhone() : existing.getPhone())
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(updated);
    }

    public void deleteUser(Long id) {
        if (!userRepository.deleteById(id)) {
            throw new UserNotFoundException(id);
        }
    }
}
