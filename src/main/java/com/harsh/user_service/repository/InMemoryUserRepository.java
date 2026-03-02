package com.harsh.user_service.repository;

import com.harsh.user_service.exception.EmailAlreadyExistsException;
import com.harsh.user_service.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    // Secondary index: normalised email -> user id for O(1) lookups and atomic uniqueness checks
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public synchronized User save(User user) {
        if (user.getId() == null) {
            // Atomic check-and-insert eliminates the TOCTOU race for new users
            String key = user.getEmail().toLowerCase();
            if (emailIndex.containsKey(key)) {
                throw new EmailAlreadyExistsException(user.getEmail());
            }
            user.setId(idSequence.getAndIncrement());
            emailIndex.put(key, user.getId());
        } else {
            // Update path: keep the email index consistent and enforce uniqueness
            User existing = store.get(user.getId());
            if (existing != null) {
                String oldKey = existing.getEmail().toLowerCase();
                String newKey = user.getEmail().toLowerCase();
                if (!oldKey.equals(newKey)) {
                    Long occupant = emailIndex.get(newKey);
                    if (occupant != null && !occupant.equals(user.getId())) {
                        throw new EmailAlreadyExistsException(user.getEmail());
                    }
                    emailIndex.remove(oldKey);
                    emailIndex.put(newKey, user.getId());
                }
            }
        }
        store.put(user.getId(), user);
        return copy(user);
    }

    @Override
    public synchronized Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(this::copy);
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        Long id = emailIndex.get(email.toLowerCase());
        return Optional.ofNullable(id).map(store::get).map(this::copy);
    }

    @Override
    public synchronized List<User> findAll(int page, int size) {
        return store.values().stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::copy)
                .toList();
    }

    @Override
    public synchronized boolean deleteById(Long id) {
        User user = store.remove(id);
        if (user != null) {
            emailIndex.remove(user.getEmail().toLowerCase());
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean existsByEmail(String email) {
        return emailIndex.containsKey(email.toLowerCase());
    }

    private User copy(User u) {
        return User.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
