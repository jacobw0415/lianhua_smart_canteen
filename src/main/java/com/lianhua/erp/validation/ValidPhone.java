package com.lianhua.erp.validation;

import com.lianhua.erp.validation.annotations.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;


import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {

    String message() default "電話格式不正確（需為 0912-345-678 或 0912345678）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}