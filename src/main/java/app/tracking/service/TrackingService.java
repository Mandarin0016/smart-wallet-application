package app.tracking.service;

import app.web.dto.PaymentNotificationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {

    @Async
    @EventListener
    public void trackNewPayment(PaymentNotificationEvent event) {

        System.out.printf("Thread [%s]: New payment for user [%s] happened.\n", Thread.currentThread().getName(), event.getUserId());
    }
}
