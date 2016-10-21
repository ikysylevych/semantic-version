package de.skuzzle.semantic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import de.skuzzle.semantic.Version.VersionFormatException;

public class VersionTest {

    private static final char[] ILLEGAL_CHAR_BOUNDS = { 'a' - 1, 'z' + 1, '0' - 1,
            '9' + 1, 'A' - 1, 'Z' + 1, '-' - 1 };

    private static final char[] ILLEGAL_NUMERIC_BOUNDS = { '0' - 1, '9' + 1 };

    private static final Version[] SEMVER_ORG_VERSIONS = new Version[] {
            Version.parseVersion("1.0.0-alpha"),
            Version.parseVersion("1.0.0-alpha.1"),
            Version.parseVersion("1.0.0-alpha.beta"),
            Version.parseVersion("1.0.0-beta"),
            Version.parseVersion("1.0.0-beta.2"),
            Version.parseVersion("1.0.0-beta.11"),
            Version.parseVersion("1.0.0-rc.1"),
            Version.parseVersion("1.0.0"),
            Version.parseVersion("2.0.0"),
            Version.parseVersion("2.1.0"),
            Version.parseVersion("2.1.1")
    };

    // same as above, but exchanged pre release and build meta data
    private static final Version[] SEMVER_ORG_BMD_VERSIONS = new Version[] {
            Version.parseVersion("1.0.0-rc.1+alpha"),
            Version.parseVersion("1.0.0-rc.1+alpha.1"),
            Version.parseVersion("1.0.0-rc.1+alpha.beta"),
            Version.parseVersion("1.0.0-rc.1+beta"),
            Version.parseVersion("1.0.0-rc.1+beta.2"),
            Version.parseVersion("1.0.0-rc.1+beta.11"),
            Version.parseVersion("1.0.0-rc.1+rc.1"),
            Version.parseVersion("1.0.0-rc.1"),
            Version.parseVersion("2.0.0-rc.1"),
            Version.parseVersion("2.1.0-rc.1"),
            Version.parseVersion("2.1.1")
    };

    private static final String[][] ILLEGAL_VERSIONS = {
            { "1.", "Incomplete version part in 1." },
            { "1.1.", "Incomplete version part in 1.1." }
    };

    public static void main(String[] args) throws IOException {
        new VersionTest().writeBinFile();
    }

    public void writeBinFile() throws IOException {
        final FileOutputStream out = new FileOutputStream("versions.bin");
        final ObjectOutputStream oout = new ObjectOutputStream(out);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            oout.writeObject(v);
        }
        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            oout.writeObject(v);
        }
        oout.close();
    }

    @Test
    public void testIllegalVersions() throws Exception {
        for (final String[] input : ILLEGAL_VERSIONS) {
            try {
                Version.parseVersion(input[0]);
                fail("String '" + input[0] + "' should not be parsable");
            } catch (final VersionFormatException e) {
                assertEquals("Expected different exception message", e.getMessage(),
                        input[1]);
            }
        }
    }

    @Test
    public void testPreReleaseEmptyString() {
        final Version v = Version.create(1, 1, 1, "");
        assertEquals("", v.getPreRelease());
        assertEquals("", v.getBuildMetaData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreReleaseNull() {
        Version.create(1, 1, 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildMDNull() {
        Version.create(1, 1, 1, "", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativePatch() {
        Version.create(1, 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMinor() {
        Version.create(1, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMajor() {
        Version.create(-1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseVersionNull() {
        Version.parseVersion(null);
    }

    @Test
    public void testSimpleVersion() {
        final Version v = Version.parseVersion("1.2.3");
        Assert.assertEquals(1, v.getMajor());
        Assert.assertEquals(2, v.getMinor());
        Assert.assertEquals(3, v.getPatch());
        Assert.assertEquals("", v.getPreRelease());
        Assert.assertEquals("", v.getBuildMetaData());
    }

    @Test
    public void testSemVerOrgPreReleaseSamples() {
        final Version v1 = Version.parseVersion("1.0.0-alpha");
        Assert.assertEquals("alpha", v1.getPreRelease());

        final Version v2 = Version.parseVersion("1.0.0-alpha.1");
        Assert.assertEquals("alpha.1", v2.getPreRelease());

        final Version v3 = Version.parseVersion("1.0.0-0.3.7");
        Assert.assertEquals("0.3.7", v3.getPreRelease());

        final Version v4 = Version.parseVersion("1.0.0-x.7.z.92");
        Assert.assertEquals("x.7.z.92", v4.getPreRelease());
    }

    @Test
    public void testSemVerOrgBuildMDSamples() {
        final Version v1 = Version.parseVersion("1.0.0-alpha+001");
        Assert.assertEquals("alpha", v1.getPreRelease());
        Assert.assertEquals("001", v1.getBuildMetaData());

        final Version v2 = Version.parseVersion("1.0.0+20130313144700");
        Assert.assertEquals("20130313144700", v2.getBuildMetaData());

        final Version v3 = Version.parseVersion("1.0.0-beta+exp.sha.5114f85");
        Assert.assertEquals("beta", v3.getPreRelease());
        Assert.assertEquals("exp.sha.5114f85", v3.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD() {
        final Version v = Version.parseVersion("1.2.3+some.id.foo");
        Assert.assertEquals("some.id.foo", v.getBuildMetaData());
    }

    @Test
    public void testVersionWithBuildMD2() {
        final Version v = Version.create(1, 2, 3, "", "some.id-1.foo");
        Assert.assertEquals("some.id-1.foo", v.getBuildMetaData());
    }

    @Test
    public void testParseMajorIsZero() throws Exception {
        final Version version = Version.parseVersion("0.1.2");
        assertEquals(0, version.getMajor());
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyLastPart() {
        Version.create(1, 2, 3, "", "some.id.");
    }

    @Test(expected = VersionFormatException.class)
    public void testVersionWithBuildMDEmptyMiddlePart() {
        Version.create(1, 2, 3, "", "some..id");
    }

    @Test
    public void testParseBuildMDWithLeadingZeroInIdentifierPart() throws Exception {
        final Version v = Version.parseVersion("1.2.3+0abc");
        assertEquals("0abc", v.getBuildMetaData());
    }

    @Test
    public void testParsePreReleaseLastPartIsNumeric() throws Exception {
        final Version v = Version.parseVersion("1.2.3-a.11+buildmd");
        assertEquals("a.11", v.getPreRelease());
        assertEquals("buildmd", v.getBuildMetaData());
    }

    @Test
    public void testVersionWithPreRelease() {
        final Version v = Version.parseVersion("1.2.3-pre.release-foo.1");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        final String[] expected = { "pre", "release-foo", "1" };
        Assert.assertArrayEquals(expected, v.getPreReleaseParts());
    }

    @Test
    public void testVersionWithPreReleaseAndBuildMD() {
        final Version v = Version
                .parseVersion("1.2.3-pre.release-foo.1+some.id-with-hyphen");
        Assert.assertEquals("pre.release-foo.1", v.getPreRelease());
        Assert.assertEquals("some.id-with-hyphen", v.getBuildMetaData());
    }

    @Test
    public void testIsValidVersionLeadingZeroMinor() throws Exception {
        assertFalse(Version.isValidVersion("1.01.1"));
    }

    private void shouldNotBeParseable(String template, char c) {
        final String v = String.format(template, c);

        try {
            Version.parseVersion(v);
            fail("Version " + v + " should not be parsable");
        } catch (final VersionFormatException e) {

        }
    }

    @Test
    public void testIllegalCharNumericParts() throws Exception {
        for (final char c : ILLEGAL_NUMERIC_BOUNDS) {
            shouldNotBeParseable("%c.2.3", c);
            shouldNotBeParseable("1.%c.3", c);
            shouldNotBeParseable("1.2.%c", c);
        }
    }

    @Test
    public void testPreReleaseInvalidChar1() throws Exception {
        for (final char c : ILLEGAL_CHAR_BOUNDS) {
            shouldNotBeParseable("1.0.0-%c", c);
        }
    }

    @Test
    public void testPreReleaseInvalidChar2() throws Exception {
        for (final char c : ILLEGAL_CHAR_BOUNDS) {
            shouldNotBeParseable("1.0.0-1.a%c", c);
        }
    }

    @Test
    public void testBuildMDInvalidChar1() throws Exception {
        for (final char c : ILLEGAL_CHAR_BOUNDS) {
            shouldNotBeParseable("1.0.0+%c", c);
        }
    }

    @Test
    public void testBuildMDInvalidChar2() throws Exception {
        for (final char c : ILLEGAL_CHAR_BOUNDS) {
            shouldNotBeParseable("1.0.0+1.a%c", c);
        }
    }

    @Test
    public void testBuildMetaDataHyphenOnly() throws Exception {
        final Version v = Version.parseVersion("1.2.3+-");
        assertEquals("-", v.getBuildMetaData());
    }

    @Test
    public void testPreReleaseHyphenOnly() throws Exception {
        final Version v = Version.parseVersion("1.2.3--");
        assertEquals("-", v.getPreRelease());
    }

    @Test(expected = VersionFormatException.class)
    public void testParseMajorUnexpectedChar() throws Exception {
        Version.parseVersion("1$.0.0");
    }

    @Test(expected = VersionFormatException.class)
    public void testParsePatchUnexpectedChar() throws Exception {
        Version.parseVersion("1.0.1$");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseLeadingZeroMinor() throws Exception {
        Version.parseVersion("1.01.1");
    }

    @Test
    public void testIsValidVersionLeadingZeroPatch() throws Exception {
        assertFalse(Version.isValidVersion("1.1.01"));
    }

    @Test(expected = VersionFormatException.class)
    public void testParseLeadingZeroPatch() throws Exception {
        Version.parseVersion("1.1.01");
    }

    @Test
    public void testIsValidVersionLeadingZeroMajor() throws Exception {
        assertFalse(Version.isValidVersion("01.1.1"));
    }

    @Test(expected = VersionFormatException.class)
    public void testParseMissingPart() throws Exception {
        Version.parseVersion("1.0");
    }

    @Test
    public void testIsValidVersionMissingPart() throws Exception {
        assertFalse(Version.isValidVersion("1.1"));
    }

    @Test(expected = VersionFormatException.class)
    public void testParsePrematureStop() throws Exception {
        Version.parseVersion("1.");
    }

    @Test
    public void testIsValidVersionPrematureStop() throws Exception {
        assertFalse(Version.isValidVersion("1."));
    }

    @Test(expected = VersionFormatException.class)
    public void testParseMajorLeadingZero() throws Exception {
        Version.parseVersion("01.0.0");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes() {
        Version.parseVersion("1.2.3-pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroes2() {
        Version.create(1, 2, 3, "pre.001");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroEOS() {
        Version.parseVersion("1.2.3-pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroEOS2() {
        Version.create(1, 2, 3, "pre.01");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseWithLeadingZeroAndBuildMD() {
        Version.parseVersion("1.2.3-pre.01+a.b");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseMiddleEmptyIdentifier() {
        Version.parseVersion("1.2.3-pre..foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier() {
        Version.parseVersion("1.2.3-pre.foo.");
    }

    @Test(expected = VersionFormatException.class)
    public void testBuildMDMiddleEmptyIdentifier() {
        Version.parseVersion("1.2.3+pre..foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testBuildMDLastEmptyIdentifier() {
        Version.parseVersion("1.2.3+pre.foo.");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoPrelease() {
        Version.parseVersion("1.2.3-foo", false);
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoBuildMetaData() {
        Version.parseVersion("1.2.3+foo", false);
    }

    @Test(expected = VersionFormatException.class)
    public void testParseExpectNoPreReleaseAndBuildMetaData() {
        Version.parseVersion("1.2.3-foo+foo", false);
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharInPreReleaseOnly() throws Exception {
        Version.parseVersion("1.2.3-$+foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharBuildMDOnly() throws Exception {
        Version.parseVersion("1.2.3+$");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharInPreRelease() throws Exception {
        Version.parseVersion("1.2.3-foo$+foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharInPreReleaseNumericPart() throws Exception {
        Version.parseVersion("1.2.3-1$+foo");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharInBuildMDNumericPart() throws Exception {
        Version.parseVersion("1.2.3-1+1$");
    }

    @Test(expected = VersionFormatException.class)
    public void testParseVersionIllegalCharInBuildMD() throws Exception {
        Version.parseVersion("1.2.3-foo+foo$");
    }

    @Test
    public void testParseVersionPreReleaseSingleZero() throws Exception {
        Version.parseVersion("1.2.3-0.1.0");
    }

    @Test
    public void testParseVersionPreReleaseAndBuildMDSingleZero() throws Exception {
        Version.parseVersion("1.2.3-0.1.0+0.0.0.1");
    }

    @Test(expected = VersionFormatException.class)
    public void testParsePreReleaseIllegalLeadingZero() throws Exception {
        Version.parseVersion("1.2.3-01.1");
    }

    @Test(expected = VersionFormatException.class)
    public void testParsePreReleaseIllegalLeadingZeroBeforeBuildMD() throws Exception {
        Version.parseVersion("1.2.3-1.01+abc");
    }

    @Test(expected = VersionFormatException.class)
    public void testParsePreReleaseIllegalLeadingZeroInLastPart() throws Exception {
        Version.parseVersion("1.2.3-1.01");
    }

    @Test
    public void testParseVersionSuccessExpectNoPreRelease() {
        Version.parseVersion("1.2.3", false);
    }

    @Test
    public void testParseVersionSuccess() {
        final Version version = Version.parseVersion("1.2.3-foo+bar", true);
        Assert.assertEquals("foo", version.getPreRelease());
        Assert.assertEquals("bar", version.getBuildMetaData());
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseLastEmptyIdentifier2() {
        Version.create(1, 2, 3, "pre.foo.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll0() {
        Version.parseVersion("0.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionAll02() {
        Version.create(0, 0, 0);
    }

    @Test(expected = VersionFormatException.class)
    public void testPreReleaseInvalid() {
        Version.create(1, 2, 3, "pre.", "build");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreReleaseNullAndBuildMDGiven() {
        Version.create(1, 2, 3, null, "build");
    }

    @Test
    public void testOnlyBuildMdEmpty() {
        Version.create(1, 2, 3, "pre", "");
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.parseVersion("1.2.3-001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testPreReleaseWithLeadingZeroesIdentifier2() {
        // leading zeroes allowed in string identifiers
        final Version v = Version.create(1, 2, 3, "001abc");
        Assert.assertEquals("001abc", v.getPreRelease());
    }

    @Test
    public void testNoPrecedenceChangeByBuildMD() {
        final Version v1 = Version.parseVersion("1.2.3+1.0");
        final Version v2 = Version.parseVersion("1.2.3+2.0");
        Assert.assertEquals(0, v1.compareTo(v2));
    }

    @Test
    public void testSimplePrecedence() {
        final Version v1 = Version.parseVersion("1.0.0");
        final Version v2 = Version.parseVersion("1.0.1");
        final Version v3 = Version.parseVersion("1.1.0");
        final Version v4 = Version.parseVersion("2.0.0");

        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v3) < 0);
        Assert.assertTrue(v3.compareTo(v4) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
        Assert.assertTrue(v3.compareTo(v2) > 0);
        Assert.assertTrue(v4.compareTo(v3) > 0);
    }

    @Test
    public void testPrecedencePreRelease() {
        final Version v1 = Version.parseVersion("1.0.0");
        final Version v2 = Version.parseVersion("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) > 0);
        Assert.assertTrue(v2.compareTo(v1) < 0);
    }

    @Test
    public void testPrecedencePreRelease2() {
        final Version v1 = Version.parseVersion("1.0.0-rc1");
        final Version v2 = Version.parseVersion("1.0.0-rc1");
        Assert.assertTrue(v1.compareTo(v2) == 0);
    }

    @Test
    public void testPrecedencePreRelease3() {
        final Version v1 = Version.parseVersion("1.0.0-rc1");
        final Version v2 = Version.parseVersion("1.0.0-rc1.5");
        // the one with longer list is greater
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease4() {
        final Version v1 = Version.parseVersion("1.0.0-a");
        final Version v2 = Version.parseVersion("1.0.0-b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease5() {
        final Version v1 = Version.parseVersion("1.0.0-1");
        final Version v2 = Version.parseVersion("1.0.0-2");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testPrecedencePreRelease6() {
        final Version v1 = Version.parseVersion("1.0.0-1.some.id-with-hyphen.a");
        final Version v2 = Version.parseVersion("1.0.0-1.some.id-with-hyphen.b");
        Assert.assertTrue(v1.compareTo(v2) < 0);
        Assert.assertTrue(v2.compareTo(v1) > 0);
    }

    @Test
    public void testInitialDevelopment() {
        final Version v1 = Version.create(0, 1, 0);
        final Version v2 = Version.create(1, 1, 0);
        Assert.assertTrue(v1.isInitialDevelopment());
        Assert.assertFalse(v2.isInitialDevelopment());
    }

    @Test
    public void testSemVerOrgPrecedenceSample() {
        for (int i = 1; i < SEMVER_ORG_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_VERSIONS[i];
            final int c = v1.compareTo(v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testSemVerOrgPrecedenceSampleComparator() {
        for (int i = 1; i < SEMVER_ORG_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_VERSIONS[i];
            final int c = Version.NATURAL_ORDER.compare(v1, v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMetaDataEquality() {
        final Version v1 = Version.create(0, 0, 1, "", "some.build-meta.data");
        final Version v2 = Version.create(0, 0, 1, "", "some.different.build-meta.data");
        Assert.assertFalse(v1.equalsWithBuildMetaData(v2));
    }

    @Test
    public void testBuildMDPrecedence() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = v1.compareToWithBuildMetaData(v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMDPrecedenceComparator() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = Version.WITH_BUILD_META_DATA_ORDER.compare(v1, v2);
            Assert.assertTrue(v1 + " is not lower than " + v2, c < 0);
        }
    }

    @Test
    public void testBuildMDPrecedenceReverse() {
        for (int i = 1; i < SEMVER_ORG_BMD_VERSIONS.length; ++i) {
            final Version v1 = SEMVER_ORG_BMD_VERSIONS[i - 1];
            final Version v2 = SEMVER_ORG_BMD_VERSIONS[i];
            final int c = v2.compareToWithBuildMetaData(v1);
            Assert.assertTrue(v2 + " is not greater than " + v1, c > 0);
        }
    }

    @Test
    public void testPreReleaseEquality() throws Exception {
        for (final Version version : SEMVER_ORG_VERSIONS) {
            final Version copy = Version.create(version.getMajor(), version.getMinor(),
                    version.getPatch(), version.getPreRelease(),
                    version.getBuildMetaData());
            assertEquals(version, copy);
            assertTrue(version.equalsWithBuildMetaData(copy));
            assertTrue(version.compareTo(copy) == 0);
            assertTrue(version.compareToWithBuildMetaData(copy) == 0);
            assertEquals(version.hashCode(), copy.hashCode());
        }
    }

    @Test
    public void testBuildMDEquality() throws Exception {
        for (final Version version : SEMVER_ORG_BMD_VERSIONS) {
            final Version copy = Version.create(version.getMajor(), version.getMinor(),
                    version.getPatch(), version.getPreRelease(),
                    version.getBuildMetaData());
            assertEquals(version, copy);
            assertTrue(version.equalsWithBuildMetaData(copy));
            assertTrue(version.compareTo(copy) == 0);
            assertTrue(version.compareToWithBuildMetaData(copy) == 0);
            assertEquals(version.hashCode(), copy.hashCode());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCompareWithBuildMDNull1() throws Exception {
        Version.compareWithBuildMetaData(null, Version.create(1, 0, 0));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareWithBuildMDNull2() throws Exception {
        Version.compareWithBuildMetaData(Version.create(1, 0, 0), null);
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull1() {
        Version.compare(null, Version.create(1, 1, 1));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull2() {
        Version.compare(Version.create(1, 1, 1), null);
    }

    @Test
    public void testCompareIdentical() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertEquals(0, Version.compare(v, v));
    }

    @Test
    public void testNotEqualsNull() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertFalse(v.equals(null));
    }

    @Test
    public void testNotEqualsForeign() {
        final Version v = Version.create(1, 1, 1);
        Assert.assertFalse(v.equals(new Object()));
    }

    @Test
    public void testEqualsIdentity() {
        final Version v = Version.create(1, 2, 3);
        Assert.assertEquals(v, v);
    }

    @Test
    public void testNotEqualsTrivial() {
        final Version v1 = Version.create(1, 1, 1);
        final Version v2 = Version.create(1, 1, 2);
        Assert.assertFalse(v1.equals(v2));
    }

    @Test
    public void testParseToString() {
        for (final Version v1 : SEMVER_ORG_VERSIONS) {
            final Version v2 = Version.parseVersion(v1.toString());
            Assert.assertEquals(v1, v2);
            Assert.assertEquals(v1.hashCode(), v2.hashCode());
        }
    }

    @Test
    public void testParseToStringUpperCase() {
        for (final Version v1 : SEMVER_ORG_VERSIONS) {
            final Version v2 = Version.parseVersion(v1.toString().toUpperCase());
            Assert.assertEquals(v1.toUpperCase(), v2);
            Assert.assertEquals(v1.toUpperCase().hashCode(), v2.hashCode());
        }
    }

    @Test
    public void testParseToStringLowerCase() {
        for (final Version v1 : SEMVER_ORG_VERSIONS) {
            final Version v2 = Version.parseVersion(v1.toString().toLowerCase());
            Assert.assertEquals(v1.toLowerCase(), v2);
            Assert.assertEquals(v1.toLowerCase().hashCode(), v2.hashCode());
        }
    }

    @Test
    public void testMin() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(0, 1, 0);

        assertSame(Version.min(v1, v2), Version.min(v2, v1));
        assertSame(v2, Version.min(v1, v2));
        assertSame(v2, v2.min(v1));
    }

    @Test
    public void testMinEquals() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(1, 0, 0);

        final Version min = Version.min(v1, v2);
        assertSame(v1, min);
        assertSame(v1, v1.min(v2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinNullV1() throws Exception {
        Version.min(null, Version.create(1, 0, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinNullV2() throws Exception {
        Version.min(Version.create(1, 0, 0), null);
    }

    @Test
    public void testMax() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(0, 1, 0);

        assertSame(Version.max(v1, v2), Version.max(v2, v1));
        assertSame(v1, Version.max(v1, v2));
        assertSame(v1, v1.max(v2));
    }

    @Test
    public void testMaxEquals() throws Exception {
        final Version v1 = Version.create(1, 0, 0);
        final Version v2 = Version.create(1, 0, 0);

        final Version max = Version.max(v1, v2);
        assertSame(v1, max);
        assertSame(v1, v1.max(v2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxNullV1() throws Exception {
        Version.max(null, Version.create(1, 0, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxNullV2() throws Exception {
        Version.max(Version.create(1, 0, 0), null);
    }

    @Test
    public void testSamePrereleaseAndWithBuildMD() throws Exception {
        final Version v1 = Version.parseVersion("1.0.0-a.b+a");
        final Version v2 = Version.parseVersion("1.0.0-a.b+b");

        assertTrue(v1.compareToWithBuildMetaData(v2) < 0);
    }

    @Test
    public void testIsNoPreReleaseIdentifierNull() throws Exception {
        assertFalse(Version.isValidPreRelease(null));
    }

    @Test
    public void testIsPreReleaseIdentifierEmptyString() throws Exception {
        assertTrue(Version.isValidPreRelease(""));
    }

    @Test
    public void testIsValidPreReleaseIdentifier() throws Exception {
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertTrue(v.getPreRelease() + " should be a valid identifier",
                    Version.isValidPreRelease(v.getPreRelease()));
        }
        assertTrue(Version.isValidPreRelease("-"));
    }

    @Test
    public void testIsNotValidPreReleaseIdentifier() throws Exception {
        assertFalse(Version.isValidPreRelease("a+b"));
    }

    @Test
    public void testIsNotValidPreReleaseNumericIdentifier() throws Exception {
        assertFalse(Version.isValidPreRelease("123+b"));
    }

    @Test
    public void testIsNotValidBuildMDIdentifier() throws Exception {
        assertFalse(Version.isValidBuildMetaData("a+b"));
    }

    @Test
    public void testIsNotValidBuildMDNumericIdentifier() throws Exception {
        assertFalse(Version.isValidBuildMetaData("123+b"));
    }

    @Test
    public void testIsNoBuildMDIdentifierNull() throws Exception {
        assertFalse(Version.isValidBuildMetaData(null));
    }

    @Test
    public void testIsBuildMDIdentifierEmptyString() throws Exception {
        assertTrue(Version.isValidBuildMetaData(""));
    }

    @Test
    public void testIsValidBuildMDIdentifier() throws Exception {
        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            assertTrue(v.toString(), Version.isValidBuildMetaData(v.getBuildMetaData()));
            assertTrue(v.toString(), Version.isValidBuildMetaData(v.getPreRelease()));
        }
        assertTrue(Version.isValidBuildMetaData("-"));
    }

    @Test
    public void testNullIsNoVersion() throws Exception {
        assertFalse(Version.isValidVersion(null));
    }

    @Test
    public void testEmptyStringIsNoVersion() throws Exception {
        assertFalse(Version.isValidVersion(""));
    }

    @Test
    public void testIsValidVersion() throws Exception {
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertTrue(Version.isValidVersion(v.toString()));
        }
    }

    @Test
    public void testSerialize() throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bout);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            out.writeObject(v);
        }
        out.close();
        final InputStream bin = new ByteArrayInputStream(bout.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bin);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertEquals(v, in.readObject());
        }
        in.close();
    }

    @Test
    public void testDeserialize05() throws Exception {
        // Deserialize a file which has been written by version 0.6.0
        final ClassLoader cl = getClass().getClassLoader();
        final InputStream inp = cl.getResourceAsStream("versions_0.6.bin");
        final ObjectInputStream oin = new ObjectInputStream(inp);
        for (final Version v : SEMVER_ORG_VERSIONS) {
            assertEquals(v, oin.readObject());
        }

        for (final Version v : SEMVER_ORG_BMD_VERSIONS) {
            assertEquals(v, oin.readObject());
        }
        oin.close();
    }

    @Test
    public void testEmptyArrayPreRelease() throws Exception {
        final Version v = Version.parseVersion("1.0.0");
        assertEquals(0, v.getPreReleaseParts().length);
    }

    @Test
    public void testEmptyArrayBuildMetaData() throws Exception {
        final Version v = Version.parseVersion("1.0.0");
        assertEquals(0, v.getBuildMetaDataParts().length);
    }

    @Test
    public void testGetBuildMDParts() throws Exception {
        final Version v = Version.parseVersion("1.0.0+a.b.c.001");
        assertArrayEquals(new String[] { "a", "b", "c", "001" },
                v.getBuildMetaDataParts());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithMajorAllWillbe0() throws Exception {
        final Version v = Version.create(1, 0, 0);
        v.withMajor(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithMinorAllWillbe0() throws Exception {
        final Version v = Version.create(0, 1, 0);
        v.withMinor(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithPatchAllWillbe0() throws Exception {
        final Version v = Version.create(0, 0, 1);
        v.withPatch(0);
    }

    @Test
    public void testWithMajorKeepEverythingElse() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo", "bar");
        assertEquals(Version.create(2, 2, 3, "foo", "bar"), v.withMajor(2));
    }

    @Test
    public void testWithMinorKeepEverythingElse() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo", "bar");
        assertEquals(Version.create(1, 1, 3, "foo", "bar"), v.withMinor(1));
    }

    @Test
    public void testWithPatchKeepEverythingElse() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo", "bar");
        assertEquals(Version.create(1, 2, 2, "foo", "bar"), v.withPatch(2));
    }

    @Test
    public void testWithPreReleaseKeepEverythingElse() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo", "bar");
        assertEquals(Version.create(1, 2, 3, "bar", "bar"), v.withPreRelease("bar"));
    }

    @Test
    public void testWithPreReleaseEmpty() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo");
        assertEquals(Version.create(1, 2, 3), v.withPreRelease(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithPreReleaseNull() throws Exception {
        Version.create(1, 2, 3).withPreRelease(null);
    }

    @Test
    public void testWithBuildMDKeepEverythingElse() throws Exception {
        final Version v = Version.create(1, 2, 3, "foo", "bar");
        assertEquals(Version.create(1, 2, 3, "foo", "foo"), v.withBuildMetaData("foo"));
    }

    @Test
    public void testWithBuildMDEmpty() throws Exception {
        final Version v = Version.create(1, 2, 3, "", "foo");
        assertEquals(Version.create(1, 2, 3), v.withBuildMetaData(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithBuildMdNull() throws Exception {
        Version.create(1, 2, 3).withBuildMetaData(null);
    }
}
