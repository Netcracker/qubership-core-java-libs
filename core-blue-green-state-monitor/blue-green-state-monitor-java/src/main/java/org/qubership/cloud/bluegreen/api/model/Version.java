package org.qubership.cloud.bluegreen.api.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version representation.
 * Helps to normalize different version syntax representation to singular, unified form: <code>v&lt;version-number&gt;</code>.
 * Also provides seamless jackson value deserialization from string supporting string and number format
 */
public class Version implements Comparable<Version> {
    private static final Pattern versionPattern = Pattern.compile("v?(\\d+)");

    private String value;
    private int intValue;

    public Version(String version) {
        if (version == null || version.isBlank()) {
            setEmpty();
        } else {
            Matcher matcher = versionPattern.matcher(version);
            if (matcher.matches()) {
                setVersion(Integer.parseInt(matcher.group(1)));
            } else {
                throw new IllegalArgumentException(String.format("invalid version number string format: '%s'", version));
            }
        }
    }

    public Version(int version) {
        setVersion(version);
    }

    public boolean isEmpty() {
        return Objects.equals(value, "");
    }

    public String value() {
        return value;
    }

    public int intValue() {
        return intValue;
    }

    private void setVersion(Integer intValue) {
        this.value = "v" + intValue;
        this.intValue = intValue;
    }

    private void setEmpty() {
        this.value = "";
        this.intValue = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Objects.equals(intValue, version.intValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intValue);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(Version o) {
        return Integer.compare(intValue, o.intValue);
    }
}
