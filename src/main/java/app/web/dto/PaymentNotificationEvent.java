package app.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentNotificationEvent {

    private UUID userId;

    private String email;

    private BigDecimal amount;

    private LocalDateTime paymentTime;

}
