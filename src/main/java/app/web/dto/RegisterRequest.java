package app.web.dto;

import app.user.model.Country;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @Size(min = 6, message = "Username must be at least 6 symbols")
    private String username;

    @Size(min = 6, message = "Password must be at least 6 symbols")
    private String password;

    @NotNull
    private Country country;
}
