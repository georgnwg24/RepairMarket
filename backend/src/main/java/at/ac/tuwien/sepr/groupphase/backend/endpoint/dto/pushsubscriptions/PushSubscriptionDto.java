package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.pushsubscriptions;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionDto {

    @NotNull(message = "Endpoint must not be null")
    private String endpoint;

    @NotNull(message = "p256dh must not be null")
    private String p256dh;

    @NotNull(message = "Auth must not be null")
    private String auth;
}
