package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Data Transfer Object (DTO) for creating a new JobOffer.
 * This class encapsulates the required attributes for job offer creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferCreateDto {

    @NotNull(message = "Price must not be null")
    @Min(value = 0, message = "Price must be non-negative")
    private Float price;
    @Size(max = 4095, message = "Comment must be at most 4095 characters")
    private String comment;
}
