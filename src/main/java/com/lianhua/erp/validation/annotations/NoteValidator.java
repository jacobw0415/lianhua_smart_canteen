package com.lianhua.erp.validation.annotations;

import com.lianhua.erp.validation.ValidNote;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class NoteValidator implements ConstraintValidator<ValidNote, String> {

    // 備註允許：
    // 中文、英文、數字、空白、常見標點符號（, . - _ ()）
    private static final String REGEX = "^[A-Za-z0-9\\u4e00-\\u9fa5 \\.,\\-_()（）]*$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // 通常備註可為空
        return value.matches(REGEX);
    }
}