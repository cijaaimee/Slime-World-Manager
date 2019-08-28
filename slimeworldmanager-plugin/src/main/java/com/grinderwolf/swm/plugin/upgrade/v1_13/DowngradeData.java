package com.grinderwolf.swm.plugin.upgrade.v1_13;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class DowngradeData {

    private final Map<String, BlockEntry> blocks;

    @Getter
    @RequiredArgsConstructor
    public static class BlockEntry {

        private final int id;
        private final int data;

        private final Map<String[], BlockProperty> properties;
        private final TileEntityData tileEntityData;

    }

    @Getter
    @RequiredArgsConstructor
    public static class BlockProperty {

        private final Map<String, BlockPropertyValue> values;

    }

    @Getter
    @RequiredArgsConstructor
    public static class BlockPropertyValue {

        private final int id;
        private final int data;

        private final PropertyOperation operation;
    }

    @Getter
    public enum PropertyOperation {
        REPLACE, OR;
    }

    @Getter
    public static class TileEntityData {

        @SerializedName("create")
        private TileCreateAction createAction;
        @SerializedName("set")
        private TileSetAction setAction;

    }

    @Getter
    public static class TileCreateAction {

        private String name;

    }

    @Getter
    @RequiredArgsConstructor
    public static class TileSetAction {

        private final Map<String, TileSetEntry> entries;

    }

    @Getter
    public static class TileSetEntry {

        private String type;
        private String value;

    }
}