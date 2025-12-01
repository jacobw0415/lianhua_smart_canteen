package com.lianhua.erp.dto.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter @Setter
public abstract class BaseRequestDto {

    /** 去除所有字串欄位前後空白 */
    public void trimAll() {
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.getType() == String.class)
                .forEach(f -> {
                    try {
                        f.setAccessible(true);
                        String value = (String) f.get(this);
                        if (value != null) {
                            f.set(this, value.trim());
                        }
                    } catch (Exception ignored) {}
                });
    }

    /** 觸發 javax validation */
    public void validateSelf() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<BaseRequestDto>> violations =
                validator.validate(this);

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)  // ⭐ 只取訊息，不取欄位
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(msg);
        }
    }
}
