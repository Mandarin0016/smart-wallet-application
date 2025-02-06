package app.scheduler;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionType;
import app.subscription.service.SubscriptionService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.web.dto.UpgradeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class SubscriptionRenewalScheduler {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionRenewalScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedDelay = 200000)
    public void renewSubscriptions() {

        List<Subscription> subscriptions = subscriptionService.getAllSubscriptionsReadyForRenewal();

        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found for renewal.");
            return;
        }

        for (Subscription subscription : subscriptions) {

            if (subscription.isRenewalAllowed()) {
                User subscriptionOwner = subscription.getOwner();
                SubscriptionType type = subscription.getType();
                SubscriptionPeriod period = subscription.getPeriod();
                UUID walletId = subscriptionOwner.getWallets().get(0).getId();
                UpgradeRequest upgradeRequest = UpgradeRequest.builder()
                        .subscriptionPeriod(period)
                        .walletId(walletId)
                        .build();
                Transaction transaction = subscriptionService.upgrade(subscriptionOwner, type, upgradeRequest);
                if (transaction.getStatus() == TransactionStatus.FAILED) {
                    subscriptionService.markSubscriptionAsTerminated(subscription);
                    subscriptionService.createDefaultSubscription(subscription.getOwner());
                }
            } else {
                subscriptionService.markSubscriptionAsCompleted(subscription);
                subscriptionService.createDefaultSubscription(subscription.getOwner());
            }
        }

    }
}
