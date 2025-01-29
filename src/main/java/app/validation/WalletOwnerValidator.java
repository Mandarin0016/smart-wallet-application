package app.validation;

import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.TransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WalletOwnerValidator implements ConstraintValidator<WalletOwner, TransferRequest> {

    private final UserService userService;

    @Autowired
    public WalletOwnerValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isValid(TransferRequest transferRequest, ConstraintValidatorContext context) {

        // Вземаме потребителя за текущата сесия
        User user = userService.getById(UUID.fromString("0a9b6944-9702-424e-b7c7-c8e82a1b5f3f"));
        UUID fromWalletId = transferRequest.getFromWalletId();

        boolean hasThisWallet = user.getWallets().stream().anyMatch(wallet -> wallet.getId().equals(fromWalletId));
        boolean isSelfTransfer = transferRequest.getToUsername().equals(user.getUsername());

        return hasThisWallet && !isSelfTransfer;
    }
}
