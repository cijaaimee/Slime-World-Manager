package com.grinderwolf.smw.api.loaders;

import java.util.HashMap;
import java.util.Map;

public class SlimeLoaders {

    private static Map<String, SlimeLoader> loaderMap = new HashMap<>();

    public static SlimeLoader get(String dataSource) {
        return loaderMap.get(dataSource);
    }

    public static void add(String dataSource, SlimeLoader loader) {
        if (loaderMap.containsKey(dataSource)) {
            throw new IllegalArgumentException("Data source " + dataSource + " already has a declared loader!");
        }

        loaderMap.put(dataSource, loader);
    }
}
