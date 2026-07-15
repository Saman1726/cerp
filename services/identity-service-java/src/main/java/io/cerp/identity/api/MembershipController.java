package io.cerp.identity.api;

import io.cerp.identity.domain.Membership;
import io.cerp.identity.service.MembershipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    @Autowired
    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public static record AssignMembershipRequest(
            @NotNull UUID tenantId,
            @NotNull UUID userId,
            @NotEmpty List<String> roles,
            @NotNull Map<String, Object> scopes) {
    }

    public static record MembershipResponse(
            UUID id,
            UUID tenantId,
            UUID userId,
            List<String> roles,
            Map<String, Object> scopes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private static MembershipResponse toResponse(Membership m) {
        return new MembershipResponse(
            m.getId(),
            m.getTenantId(),
            m.getUserId(),
            m.getRoles(),
            m.getScopes(),
            m.getCreatedAt(),
            m.getUpdatedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse assign(@Valid @RequestBody AssignMembershipRequest body) {
        Membership saved = membershipService.assign(body.tenantId(), body.userId(), body.roles(), body.scopes());
        return toResponse(saved);
    }

    @GetMapping
    public Object get(
            @RequestParam(value = "tenantId", required = false) UUID tenantId,
            @RequestParam(value = "userId", required = false) UUID userId) {
        if (tenantId != null && userId != null) {
            Optional<Membership> m = membershipService.findByTenantAndUser(tenantId, userId);
            return m.map(MembershipController::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for tenant=" + tenantId + " user=" + userId));
        }
        if (tenantId != null) {
            return membershipService.listByTenant(tenantId).stream().map(MembershipController::toResponse).toList();
        }
        if (userId != null) {
            return membershipService.listByUser(userId).stream().map(MembershipController::toResponse).toList();
        }
        throw new BadRequestException("Provide tenantId and userId for single membership, or one of tenantId/userId to list");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) {
            super(msg);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequestException extends RuntimeException {
        public BadRequestException(String msg) {
            super(msg);
        }
    }
}
