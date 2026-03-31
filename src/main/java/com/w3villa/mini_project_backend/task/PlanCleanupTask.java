package com.w3villa.mini_project_backend.task;

import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanCleanupTask {

    private final UserRepository userRepository;

    // Runs every 5 minutes
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void expireUserPlans() {
        // Correct way to get the current moment as an Instant
        Instant now = Instant.now();

        // Match the parameter type to what your JpaRepository expects
        List<User> expiredUsers = userRepository.findByPlanExpiryBeforeAndPlanTypeNot(now, PlanType.FREE);

        for (User user : expiredUsers) {
            user.setPlanType(PlanType.FREE);
            user.setPlanExpiry(null);
            userRepository.save(user);
            System.out.println("❌ SESSION EXPIRED: Revoking access for user " + user.getEmail());
        }
    }
}