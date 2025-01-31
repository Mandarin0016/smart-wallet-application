package app.web;

import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @GetMapping
    public ModelAndView getAllTransactions(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");
        List<Transaction> transactions = transactionService.getAllByOwnerId(userId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("transactions");
        modelAndView.addObject("transactions", transactions);

        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView getTransactionById(@PathVariable UUID id) {

        Transaction transaction = transactionService.getById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("transaction-result");
        modelAndView.addObject("transaction", transaction);

        return modelAndView;
    }
}
