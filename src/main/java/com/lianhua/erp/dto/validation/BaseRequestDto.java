package com.lianhua.erp.dto.validation;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Getter @Setter
public abstract class BaseRequestDto {

    public void trimAll() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(String.class)) {
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(this);
                    if (value != null) field.set(this, value.trim());
                } catch (Exception ignored) {}
            }
        }
    }
}
