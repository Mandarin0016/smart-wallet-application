package app.subscription.repository;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByStatusAndOwnerId(SubscriptionStatus status, UUID ownerId);

    List<Subscription> findAllByStatusAndCompletedOnLessThanEqual(SubscriptionStatus status, LocalDateTime now);
}