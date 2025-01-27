package app.transaction.service;

import app.exception.DomainException;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createNewTransaction(User owner, String sender, String receiver, BigDecimal transactionAmount, BigDecimal balanceLeft, Currency currency, TransactionType type, TransactionStatus status, String transactionDescription, String failureReason) {

        Transaction transaction = Transaction.builder()
                .owner(owner)
                .sender(sender)
                .receiver(receiver)
                .amount(transactionAmount)
                .balanceLeft(balanceLeft)
                .currency(currency)
                .type(type)
                .status(status)
                .description(transactionDescription)
                .failureReason(failureReason)
                .createdOn(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllByOwnerId(UUID ownerId) {

        return transactionRepository.findAllByOwnerIdOrderByCreatedOnDesc(ownerId);
    }

    public Transaction getById(UUID id) {

        return transactionRepository.findById(id).orElseThrow(() -> new DomainException("Transaction with id [%s] does not exist.".formatted(id)));
    }
}
