package app;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.user.model.Country;
import app.user.model.User;
import app.user.model.UserRole;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static User aRandomUser() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("User")
                .password("123123")
                .role(UserRole.USER)
                .country(Country.BULGARIA)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .currency(Currency.getInstance("EUR"))
                .updatedOn(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .build();

        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .type(SubscriptionType.PREMIUM)
                .price(BigDecimal.ZERO)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .createdOn(LocalDateTime.now())
                .completedOn(LocalDateTime.now().plusMonths(1))
                .owner(user)
                .renewalAllowed(true)
                .build();

        user.setSubscriptions(List.of(subscription));
        user.setWallets(List.of(wallet));

        return user;
    }
}
