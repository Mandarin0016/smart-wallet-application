package app.wallet.service;

import app.exception.DomainException;
import app.notification.service.NotificationService;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionType;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.web.dto.PaymentNotificationEvent;
import app.web.dto.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class WalletService {

    private static final String SMART_WALLET_LTD = "Smart Wallet Ltd";

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public WalletService(WalletRepository walletRepository,
                         TransactionService transactionService,
                         ApplicationEventPublisher eventPublisher) {

        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    public void unlockNewWallet(User user) {

        List<Wallet> allUserWallets = walletRepository.findAllByOwnerUsername(user.getUsername());
        Subscription activeSubscription = user.getSubscriptions().get(0);

        // Pesho
        // Subs: Premium
        // Wallets: 2

        boolean isDefaultPlanAndMaxWalletsUnlocked = activeSubscription.getType() == SubscriptionType.DEFAULT && allUserWallets.size() == 1;
        boolean isPremiumPlanAndMaxWalletsUnlocked = activeSubscription.getType() == SubscriptionType.PREMIUM && allUserWallets.size() == 2;
        boolean isUltimatePlanAndMaxWalletsUnlocked = activeSubscription.getType() == SubscriptionType.ULTIMATE && allUserWallets.size() == 3;

        if (isDefaultPlanAndMaxWalletsUnlocked || isPremiumPlanAndMaxWalletsUnlocked || isUltimatePlanAndMaxWalletsUnlocked) {
            throw new DomainException("Max wallet count reached for user with id [%s] and subscription type [%s]".formatted(user.getId(), activeSubscription.getType()));
        }

        Wallet newWallet = Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal(0))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        walletRepository.save(newWallet);
    }

    public Wallet initilizeFirstWallet(User user) {

        List<Wallet> allUserWallets = walletRepository.findAllByOwnerUsername(user.getUsername());
        if (!allUserWallets.isEmpty()) {
            throw new DomainException("User with id [%s] already has wallets. First wallet can't be initialized.".formatted(user.getId()));
        }

        Wallet wallet = walletRepository.save(initializeWallet(user));
        log.info("Successfully create new wallet with id [%s] and balance [%.2f].".formatted(wallet.getId(), wallet.getBalance()));

        return wallet;
    }

    @Transactional
    public Transaction topUp(UUID walletId, BigDecimal amount) {

        Wallet wallet = getWalletById(walletId);
        String transactionDescription = "Top up %.2f".formatted(amount.doubleValue());

        if (wallet.getStatus() == WalletStatus.INACTIVE) {

            return transactionService.createNewTransaction(wallet.getOwner(),
                    SMART_WALLET_LTD,
                    walletId.toString(),
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.DEPOSIT,
                    TransactionStatus.FAILED,
                    transactionDescription,
                    "Inactive wallet");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(wallet);

        return transactionService.createNewTransaction(wallet.getOwner(),
                SMART_WALLET_LTD,
                walletId.toString(),
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transactionDescription,
                null);
    }

    public Transaction transferFunds(User sender, TransferRequest transferRequest) {

        Wallet senderWallet = getWalletById(transferRequest.getFromWalletId());
        Optional<Wallet> receiverWalletOptional = walletRepository.findAllByOwnerUsername(transferRequest.getToUsername())
                .stream()
                .filter(w -> w.getStatus() == WalletStatus.ACTIVE)
                .findFirst();

        String transferDescription = "Transfer from %s to %s, for %.2f".formatted(sender.getUsername(), transferRequest.getToUsername(), transferRequest.getAmount());

        if (receiverWalletOptional.isEmpty()) {

            return transactionService.createNewTransaction(sender,
                    senderWallet.getId().toString(),
                    transferRequest.getToUsername(),
                    transferRequest.getAmount(),
                    senderWallet.getBalance(),
                    senderWallet.getCurrency(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    transferDescription,
                    "Invalid criteria for transfer");
        }

        // Money Transfer
        // Ivan -> Gosho | 20 EUR
        // Ivan -20.00 EUR
        // Gosho +20.00 EUR

        Transaction withdrawal = charge(sender, senderWallet.getId(), transferRequest.getAmount(), transferDescription);
        if (withdrawal.getStatus() == TransactionStatus.FAILED) {
            return withdrawal;
        }

        Wallet receiverWallet = receiverWalletOptional.get();
        receiverWallet.setBalance(receiverWallet.getBalance().add(transferRequest.getAmount()));
        receiverWallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(receiverWallet);
        transactionService.createNewTransaction(receiverWallet.getOwner(),
                senderWallet.getId().toString(),
                receiverWallet.getId().toString(),
                transferRequest.getAmount(),
                receiverWallet.getBalance(),
                receiverWallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transferDescription,
                null);

        return withdrawal;
    }

    @Transactional
    public Transaction charge(User user, UUID walletId, BigDecimal amount, String description) {

        Wallet wallet = getWalletById(walletId);

        String failureReason = null;
        boolean isFailedTransaction = false;
        if (wallet.getStatus() == WalletStatus.INACTIVE) {
            failureReason = "Inactive wallet status";
            isFailedTransaction = true;
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            failureReason = "Insufficient funds";
            isFailedTransaction = true;
        }

        if (isFailedTransaction) {
            return transactionService.createNewTransaction(user,
                    wallet.getId().toString(),
                    SMART_WALLET_LTD,
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    description,
                    failureReason);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(wallet);

        // Успешно плащане
        System.out.printf("Thread [%s]: Code in WalletService.class\n", Thread.currentThread().getName());
        PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                .userId(user.getId())
                .paymentTime(LocalDateTime.now())
                .email(user.getEmail())
                .amount(amount)
                .build();
        // Ако искате да публикувате евент, просто разкоментирайте реда по-долу
        // eventPublisher.publishEvent(event);

        return transactionService.createNewTransaction(user,
                wallet.getId().toString(),
                SMART_WALLET_LTD,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCEEDED,
                description,
                null);
    }

    private Wallet getWalletById(UUID walletId) {

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new DomainException("Wallet with id [%s] does not exist.".formatted(walletId)));
    }

    private Wallet initializeWallet(User user) {

        return Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal("20.00"))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    public Map<UUID, List<Transaction>> getLastFourTransactions(List<Wallet> wallets) {

        Map<UUID, List<Transaction>> transactionsByWalletId = new LinkedHashMap<>();

        for (Wallet wallet : wallets) {

            List<Transaction> lastFourTransactions = transactionService.getLastFourTransactionsByWallet(wallet);
            transactionsByWalletId.put(wallet.getId(), lastFourTransactions);
        }

        return transactionsByWalletId;
    }

    public void switchStatus(UUID walletId, UUID ownerId) {

        Optional<Wallet> optionalWallet = walletRepository.findByIdAndOwnerId(walletId, ownerId);
        if (optionalWallet.isEmpty()){
            throw new DomainException("Wallet with id [%s] does not belong to user with id [%s]".formatted(walletId, ownerId));
        }

        Wallet wallet = optionalWallet.get();
        if (wallet.getStatus() == WalletStatus.ACTIVE){
            wallet.setStatus(WalletStatus.INACTIVE);
        } else {
            wallet.setStatus(WalletStatus.ACTIVE);
        }
        walletRepository.save(wallet);
    }
}
