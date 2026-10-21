package at.ac.tuwien.sepr.groupphase.backend.validation;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserRegistrationDto> {

    public boolean isValid(UserRegistrationDto userRegistrationDto, ConstraintValidatorContext context) {
        if (userRegistrationDto == null || userRegistrationDto.getPassword() == null) {
            return false;
        }
        return userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword());
    }
}
