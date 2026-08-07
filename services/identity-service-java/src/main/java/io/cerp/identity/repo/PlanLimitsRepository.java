package io.cerp.identity.repo;

import io.cerp.identity.domain.Plan;
import io.cerp.identity.domain.PlanLimits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanLimitsRepository extends JpaRepository<PlanLimits, Plan> {
}
