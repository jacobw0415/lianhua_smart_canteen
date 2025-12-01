package com.lianhua.erp.validation;

import com.lianhua.erp.validation.annotations.NoteValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoteValidator.class)
public @interface ValidNote {

    String message() default "備註不得包含不合法字元";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
