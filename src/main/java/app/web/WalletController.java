package app.web;

import app.security.AuthenticationMetadata;
import app.transaction.model.Transaction;
import app.user.model.User;
import app.user.service.UserService;
import app.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/wallets")
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;

    @Autowired
    public WalletController(UserService userService, WalletService walletService) {

        this.userService = userService;
        this.walletService = walletService;
    }

    @GetMapping
    public ModelAndView getWalletsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById(authenticationMetadata.getUserId());
        Map<UUID, List<Transaction>> lastFourTransactionsPerWallet = walletService.getLastFourTransactions(user.getWallets());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("wallets");
        modelAndView.addObject("user", user);
        modelAndView.addObject("lastFourTransactions", lastFourTransactionsPerWallet);

        return modelAndView;
    }

    @PostMapping
    public String createNewWallet(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.getById(authenticationMetadata.getUserId());
        walletService.unlockNewWallet(user);

        return "redirect:/wallets";
    }

    @PutMapping("/{id}/status")
    public String switchWalletStatus(@PathVariable UUID id, @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        walletService.switchStatus(id, authenticationMetadata.getUserId());

        return "redirect:/wallets";
    }

    @PutMapping("/{id}/balance/top-up")
    public String topUpWalletBalance(@PathVariable UUID id) {

        Transaction transaction = walletService.topUp(id, BigDecimal.valueOf(20));

        return "redirect:/transactions/" + transaction.getId();
    }
}
