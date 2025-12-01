package com.lianhua.erp.validation.annotations;

import com.lianhua.erp.validation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    // 允許：空字串、null（因為 phone 通常不是強制）
    // 允許：0912-345-678、0912345678
    private static final String REGEX = "^$|^09\\d{2}-?\\d{3}-?\\d{3}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.matches(REGEX);
    }
}
