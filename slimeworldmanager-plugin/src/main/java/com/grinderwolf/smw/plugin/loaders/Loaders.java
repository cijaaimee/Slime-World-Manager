package com.grinderwolf.smw.plugin.loaders;

import com.grinderwolf.smw.api.SlimeLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Loaders {
    FILE(new FileLoader());

    private final SlimeLoader instance;
}
