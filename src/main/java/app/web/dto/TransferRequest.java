package app.web.dto;

import app.validation.WalletOwner;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@WalletOwner
public class TransferRequest {

    @NotNull
    private UUID fromWalletId;

    @NotNull
    private String toUsername;

    @NotNull
    @Positive
    private BigDecimal amount;
}
