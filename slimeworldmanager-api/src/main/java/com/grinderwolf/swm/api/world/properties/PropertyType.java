package com.grinderwolf.swm.api.world.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyType {
    STRING(String.class), BOOLEAN(Boolean.class), INT(Integer.class);

    private final Class<?> valueClazz;
}
