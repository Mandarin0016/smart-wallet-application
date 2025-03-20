package app;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.repository.SubscriptionRepository;
import app.subscription.service.SubscriptionService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.Country;
import app.user.model.User;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import app.web.dto.RegisterRequest;
import app.web.dto.UpgradeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest // Integration Test (Load the complete Spring Application Context - all beans)
public class Subscribe2ITest {

    // Current Test Coverage: 29%
    // After Integration Test: 50%

    // What do I need to subscribe?
    // - registered user

    // ВАЖНО: 'user' key word in H2

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void subscribeToPlan_happyPath() {

        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("Vik123")
                .password("123123")
                .country(Country.BULGARIA)
                .build();
        // Wallet has 20 EUR
        User registeredUser = userService.register(registerRequest);
        UpgradeRequest upgradeRequest = UpgradeRequest.builder()
                .subscriptionPeriod(SubscriptionPeriod.MONTHLY)
                .walletId(registeredUser.getWallets().get(0).getId())
                .build();

        // When
        Transaction transaction = subscriptionService.upgrade(registeredUser, SubscriptionType.PREMIUM, upgradeRequest);

        // Then
        // 1. SUCCEEDED transaction status
        assertThat(transaction.getStatus(), is(TransactionStatus.SUCCEEDED));
        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
        // 2. User has ONE ACTIVE subscription - PREMIUM
        Optional<Subscription> premiumSubscription = subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, registeredUser.getId());
        assertTrue(premiumSubscription.isPresent());
        assertEquals(SubscriptionType.PREMIUM, premiumSubscription.get().getType());

        // 3. User's wallet has less money
        Optional<Wallet> userWallet = walletRepository.findByIdAndOwnerId(registeredUser.getWallets().get(0).getId(), registeredUser.getId());
        assertTrue(userWallet.isPresent());
        // 20.00 EUR - 19.99 EUR = 0.01 EUR
        assertThat(userWallet.get().getBalance(), comparesEqualTo(new BigDecimal("0.01")));
    }
}
