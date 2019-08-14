package com.grinderwolf.swm.plugin.update;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Version implements Comparable<Version> {

    private static final Pattern PATTERN = Pattern.compile("(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)(?:\\.(?<patch>0|[1-9]\\d*))?(?:-(?<tag>[A-z0-9.-]*))?");

    private final int[] version = new int[3];
    private final String tag;

    Version(String value) {
        Matcher matcher = PATTERN.matcher(value);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid version format " + value);
        }

        version[0] = Integer.parseInt(matcher.group("major"));
        version[1] = Integer.parseInt(matcher.group("minor"));
        version[2] = Integer.parseInt(matcher.group("patch"));
        tag = matcher.group("tag") != null ? matcher.group("tag") : "";
    }

    @Override
    public int compareTo(Version other) {
        if (other == this) {
            return 0;
        }

        if (other == null) {
            return 1;
        }

        for (int i = 0; i < 3; i++) {
            int partA = version[i];
            int partB = other.getVersion()[i];

            if (partA > partB) {
                return 1;
            }

            if (partA < partB) {
                return -1;
            }
        }

        if (tag.length() == 0 && other.getTag().length() > 0) {
            return -1;
        }

        if (tag.length() > 0 && other.getTag().length() == 0) {
            return 1;
        }

        return 0;
    }
}
