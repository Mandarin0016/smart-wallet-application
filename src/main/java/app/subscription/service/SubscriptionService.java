package app.subscription.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.repository.SubscriptionRepository;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.wallet.service.WalletService;
import app.web.dto.UpgradeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               WalletService walletService) {

        this.subscriptionRepository = subscriptionRepository;
        this.walletService = walletService;
    }

    public Subscription createDefaultSubscription(User user) {

        Subscription subscription = subscriptionRepository.save(initializeSubscription(user));
        log.info("Successfully create new subscription with id [%s] and type [%s].".formatted(subscription.getId(), subscription.getType()));

        return subscription;
    }

    private Subscription initializeSubscription(User user) {

        LocalDateTime now = LocalDateTime.now();

        return Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .type(SubscriptionType.DEFAULT)
                .price(new BigDecimal("0.00"))
                .renewalAllowed(true)
                .createdOn(now)
                .completedOn(now.plusMonths(1))
                .build();
    }

    @Transactional
    public Transaction upgrade(User user, SubscriptionType subscriptionType, UpgradeRequest upgradeRequest) {

        Optional<Subscription> optionalSubscription = subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, user.getId());
        if (optionalSubscription.isEmpty()) {
            throw new DomainException("No active subscription has been found for user with id [%s]".formatted(user.getId()));
        }

        Subscription currentSubscription = optionalSubscription.get();

        // Purchase of Monthly Premium subscription
        SubscriptionPeriod subscriptionPeriod = upgradeRequest.getSubscriptionPeriod();
        String period = subscriptionPeriod.name().substring(0, 1).toUpperCase() + subscriptionPeriod.name().substring(1);
        String type = subscriptionType.name().substring(0, 1).toUpperCase() + subscriptionType.name().substring(1);
        String chargeDescription = "Purchase of %s %s subscription".formatted(period, type);
        BigDecimal subscriptionPrice = getSubscriptionPrice(subscriptionType, subscriptionPeriod);

        Transaction chargeResult = walletService.charge(user, upgradeRequest.getWalletId(), subscriptionPrice, chargeDescription);
        if (chargeResult.getStatus() == TransactionStatus.FAILED) {
            log.warn("Charge for subscription failed for user with id [%s], subscription type [%s]".formatted(user.getId(), subscriptionType));
            return chargeResult;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime completedOn;
        if (subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            completedOn= now.plusMonths(1);
        } else {
            completedOn= now.plusYears(1);
        }

        Subscription newSubscription = Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(subscriptionPeriod)
                .type(subscriptionType)
                .price(subscriptionPrice)
                .renewalAllowed(subscriptionPeriod == SubscriptionPeriod.MONTHLY)
                .createdOn(now)
                .completedOn(completedOn)
                .build();

        currentSubscription.setCompletedOn(now);
        currentSubscription.setStatus(SubscriptionStatus.COMPLETED);

        subscriptionRepository.save(currentSubscription);
        subscriptionRepository.save(newSubscription);

        return chargeResult;
    }

    private BigDecimal getSubscriptionPrice(SubscriptionType subscriptionType, SubscriptionPeriod subscriptionPeriod) {

        if (subscriptionType == SubscriptionType.DEFAULT) {
            return BigDecimal.ZERO;
        } else if (subscriptionType == SubscriptionType.PREMIUM && subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            return new BigDecimal("19.99");
        } else if (subscriptionType == SubscriptionType.PREMIUM && subscriptionPeriod == SubscriptionPeriod.YEARLY) {
            return new BigDecimal("199.99");
        } else if (subscriptionType == SubscriptionType.ULTIMATE && subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            return new BigDecimal("49.99");
        } else {
            return new BigDecimal("499.99");
        }
    }

    public List<Subscription> getAllSubscriptionsReadyForRenewal() {

        return subscriptionRepository.findAllByStatusAndCompletedOnLessThanEqual(SubscriptionStatus.ACTIVE, LocalDateTime.now());
    }

    public void markSubscriptionAsCompleted(Subscription subscription) {

        subscription.setStatus(SubscriptionStatus.COMPLETED);
        subscription.setCompletedOn(LocalDateTime.now());

        subscriptionRepository.save(subscription);
    }

    public void markSubscriptionAsTerminated(Subscription subscription) {

        subscription.setStatus(SubscriptionStatus.TERMINATED);
        subscription.setCompletedOn(LocalDateTime.now());

        subscriptionRepository.save(subscription);
    }
}
