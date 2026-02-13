package com.gasagency.validation;

import com.gasagency.dto.response.CustomerVariantPriceDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class PriceValidator implements ConstraintValidator<ValidPrices, CustomerVariantPriceDTO> {

    @Override
    public void initialize(ValidPrices annotation) {
    }

    @Override
    public boolean isValid(CustomerVariantPriceDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        BigDecimal salePrice = dto.getSalePrice();
        BigDecimal discountPrice = dto.getDiscountPrice();

        // Both prices must be provided
        if (salePrice == null || discountPrice == null) {
            return false;
        }

        // Discount price cannot be greater than sale price
        if (discountPrice.compareTo(salePrice) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Discount price cannot be greater than sale price")
                    .addPropertyNode("discountPrice")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

