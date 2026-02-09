package com.gasagency.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PriceValidator.class)
@Documented
public @interface ValidPrices {
    String message() default "Discount price cannot be greater than sale price";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
