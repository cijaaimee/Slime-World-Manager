package com.grinderwolf.smw.api.loaders;

import java.util.HashMap;
import java.util.Map;

public class SlimeLoaders {

    private static Map<String, SlimeLoader> loaderMap = new HashMap<>();

    public static SlimeLoader get(String loaderName) {
        return loaderMap.get(loaderName);
    }

    public static void add(String loaderName, SlimeLoader loader) {
        if (loaderMap.containsKey(loaderName)) {
            throw new IllegalArgumentException("Loader " + loaderName + " already exists!");
        }

        if (loaderMap.containsValue(loader)) {
            throw new IllegalArgumentException("Loader " + loaderName + " is already registered with another name!");
        }

        loaderMap.put(loaderName, loader);
    }
}
