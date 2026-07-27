package io.cerp.identity.service;

import io.cerp.identity.domain.Membership;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.events.IdentityEventPublisher;
import io.cerp.identity.repo.MembershipRepository;
import io.cerp.identity.repo.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserAccountRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final @Nullable IdentityEventPublisher eventPublisher;

    @Autowired
    public UserService(UserAccountRepository userRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) IdentityEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
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

    /**
     * Create a user with a password. The password will be BCrypt-hashed before storage.
     */
    public UserAccount createWithPassword(String email, String name, String rawPassword) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("User with email already exists: " + email);
        });

        UserAccount user = new UserAccount(null, email, name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        UserAccount saved = userRepository.save(user);

        if (eventPublisher != null) {
            eventPublisher.publishUserCreated(saved);
        }
        return saved;
    }

    /**
     * Verify a user's password against the stored hash. Returns true if the password matches.
     */
    public boolean verifyPassword(UserAccount user, String rawPassword) {
        String hash = user.getPasswordHash();
        if (hash == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hash);
    }

    /**
     * Get user details including password hash for authentication. This should only be called from authentication flows.
     */
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmailForAuth(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get user details by name (username) for authentication. This should only be called from authentication flows.
     */
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByNameForAuth(String name) {
        return userRepository.findByName(name);
    }

    /**
     * Get user's roles from their memberships.
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(UUID userId) {
        return membershipRepository.findByUserId(userId)
            .stream()
            .flatMap(m -> m.getRoles().stream())
            .distinct()
            .toList();
    }

    /**
     * Get the user's primary tenant ID from their first membership.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> getUserTenantId(UUID userId) {
        return membershipRepository.findByUserId(userId)
            .stream()
            .findFirst()
            .map(Membership::getTenantId);
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

    /**
     * Save (create or update) a user account.
     */
    public UserAccount save(UserAccount user) {
        return userRepository.save(user);
    }
}
