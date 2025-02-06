package app.email.service;

import app.web.dto.PaymentNotificationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Async
    @EventListener
    public void sendEmailWhenChargeHappen(PaymentNotificationEvent event) {

        System.out.printf("Thread [%s]: Charge happened for user with id [%s].\n", Thread.currentThread().getName(), event.getUserId());
    }
}
