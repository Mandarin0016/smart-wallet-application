package app.web.dto;

import app.subscription.model.SubscriptionPeriod;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UpgradeRequest {

    private SubscriptionPeriod subscriptionPeriod;

    private UUID walletId;
}
