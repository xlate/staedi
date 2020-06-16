package io.xlate.edi.internal.schema;

class VersionedProperty {

    final String minVersion;
    final String maxVersion;

    public VersionedProperty(String minVersion, String maxVersion) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    boolean appliesTo(String version) {
        return minVersionIncludes(version) && maxVersionIncludes(version);
    }

    boolean minVersionIncludes(String version) {
        return minVersion.trim().isEmpty() || minVersion.compareTo(version) <= 0;
    }

    boolean maxVersionIncludes(String version) {
        return maxVersion.trim().isEmpty() || maxVersion.compareTo(version) >= 0;
    }

}
