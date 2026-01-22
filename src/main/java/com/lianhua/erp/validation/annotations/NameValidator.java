package com.lianhua.erp.validation.annotations;
import com.lianhua.erp.validation.ValidName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    private static final String REGEX = "^(?!\\d+$)[A-Za-z0-9\\u4e00-\\u9fa5 ]+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.matches(REGEX) && !value.trim().isEmpty();
    }
}
