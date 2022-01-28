package com.battybuilds.webclientoauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validateValidRequest() {
        AlarmRequest request = AlarmRequest.builder().year(1911).build();
        Set<ConstraintViolation<AlarmRequest>> errors = validator.validate(request);
        assertThat(errors.size()).as("errors thrown were: " + extractErrors(errors)).isZero();
    }

    @Test
    void requestFailsValidation() {
        AlarmRequest request = AlarmRequest.builder().build();
        Set<ConstraintViolation<AlarmRequest>> errors = validator.validate(request);
        assertThat(errors.size()).as("did not validate request").isNotZero();
        assertErrorWithMessage("year is required", errors);
    }

    private void assertErrorWithMessage(String errorMessage, Set<ConstraintViolation<AlarmRequest>> errors) {
        try {
            assertThat(errors.stream().findFirst().get().getMessage()).contains((errorMessage));
        } catch (NoSuchElementException e) {
            fail("Did not validate field");
        } catch (Exception e) {
            fail("Some other exception was: " + e.getMessage());
        }
    }

    private String extractErrors(Set<ConstraintViolation<AlarmRequest>> errors) {
        StringBuilder errorMessages = new StringBuilder();
        for (ConstraintViolation<?> error : errors) {
            errorMessages.append(error.getMessage()).append(", ");
        }
        return errorMessages.toString();
    }
}
