package io.cerp.identity.service;

import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.events.IdentityEventPublisher;
import io.cerp.identity.repo.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserAccountRepository userRepository;
    private final @Nullable IdentityEventPublisher eventPublisher;

    @Autowired
    public UserService(UserAccountRepository userRepository, @Autowired(required = false) IdentityEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public UserAccount create(String email, String name) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("User with email already exists: " + email);
        });

        UserAccount user = new UserAccount(null, email, name);
        UserAccount saved = userRepository.save(user);

        if (eventPublisher != null) {
            eventPublisher.publishUserCreated(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userRepository.findAll();
    }
}
