package io.cerp.catalog.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Binds the current request's tenant id (from TenantContext) into the PostgreSQL session/transaction using SELECT set_config('app.tenant_id', ..., true) so that RLS
 * policies can enforce tenant isolation.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RlsTenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Wrap service layer calls to ensure the DB session/transaction is tagged with tenant id. The @Transactional here ensures a transaction is present so set_config is
     * LOCAL to the tx.
     */
    @Around("execution(public * io.cerp.catalog.service..*(..))")
    @Transactional
    public Object bindTenant(ProceedingJoinPoint pjp) throws Throwable {
        Optional<UUID> current = TenantContext.get();
        if (current.isPresent()) {
            entityManager
                .createNativeQuery("select set_config('app.tenant_id', :tid, true)")
                .setParameter("tid", current.get().toString())
                .getSingleResult();
        }
        return pjp.proceed();
    }
}
