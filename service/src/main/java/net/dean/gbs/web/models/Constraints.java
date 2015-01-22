package net.dean.gbs.web.models;

import java.util.regex.Pattern;

public final class Constraints {
    public static final int DIR_MAX_LENGTH = 200;
    public static final int DIR_MIN_LENGTH = 1;

    public static final int NAME_MAX_LENGTH = DIR_MAX_LENGTH;
    public static final int NAME_MIN_LENGTH = 3;

    public static final String GROUP_REGEX = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    public static final Pattern GROUP_PATTERN = Pattern.compile(GROUP_REGEX);
    public static final int GROUP_MAX_LENGTH = 10000;
    public static final int GROUP_MIN_LENGTH = 1;

    public static final int VERSION_MAX_LENGTH = 200;
    public static final int VERSION_MIN_LENGTH = 0;
}
