package com.twelvemonkeys.util;

import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class UUIDFactoryTest {
    private static final String EXAMPLE_COM_UUID = "http://www.example.com/uuid/";

    // Nil UUID

    @Test
    public void testNilUUID() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID b = new UUID(0l, 0l);

        assertEquals(UUIDFactory.NIL, b);
        assertEquals(UUIDFactory.NIL, a);
        assertEquals(a, b); // Sanity

        assertEquals(0, UUIDFactory.NIL.variant());
        assertEquals(0, UUIDFactory.NIL.version());
    }

    // Version 3 UUIDs (for comparison with v5)

    @Test
    public void testVersion3NameBasedMD5VariantVersion() throws UnsupportedEncodingException {
        UUID a = UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(2, a.variant());
        assertEquals(3, a.version());
    }

    @Test
    public void testVersion3NameBasedMD5Equals() throws UnsupportedEncodingException {
        UUID a = UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        UUID b = UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(a, b);
    }

    @Test
    public void testVersion3NameBasedMD5NotEqualSHA1() throws UnsupportedEncodingException {
        UUID a = UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertFalse(a.equals(UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"))));
    }

    @Test
    public void testVersion3NameBasedMD5FromStringRep() throws UnsupportedEncodingException {
        UUID a = UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(a, UUID.fromString(a.toString()));
    }

    // Version 5 UUIDs

    @Test
    public void testVersion5NameBasedSHA1VariantVersion() throws UnsupportedEncodingException {
        UUID a = UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(2, a.variant());
        assertEquals(5, a.version());
    }

    @Test
    public void testVersion5NameBasedSHA1Equals() throws UnsupportedEncodingException {
        UUID a = UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        UUID b = UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(a, b);
    }

    @Test
    public void testVersion5NameBasedSHA1NotEqualMD5() throws UnsupportedEncodingException {
        UUID a = UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertFalse(a.equals(UUID.nameUUIDFromBytes(EXAMPLE_COM_UUID.getBytes("UTF-8"))));
    }

    @Test
    public void testVersion5NameBasedSHA1FromStringRep() throws UnsupportedEncodingException {
        UUID a = UUIDFactory.nameUUIDFromBytesSHA1(EXAMPLE_COM_UUID.getBytes("UTF-8"));
        assertEquals(a, UUID.fromString(a.toString()));
    }

    // Version 1 UUIDs

    @Test
    public void testVersion1NodeBasedVariantVersion() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        assertEquals(2, uuid.variant());
        assertEquals(1, uuid.version());
    }

    @Test
    public void testVersion1NodeBasedMacAddress() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        assertEquals(UUIDFactory.MAC_ADDRESS_NODE, uuid.node());
        // TODO: Test that this is actually a Mac address from the local computer, or specified through system property?
    }

    @Test
    public void testVersion1NodeBasedFromStringRep() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        assertEquals(uuid, UUID.fromString(uuid.toString()));
    }

    @Test
    public void testVersion1NodeBasedClockSeq() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        assertEquals(UUIDFactory.Clock.getClockSequence(), uuid.clockSequence());

        // Test time fields (within reasonable limits +/- 100 ms or so?)
        // TODO: Compare with system clock for sloppier resolution
        assertEquals(UUIDFactory.Clock.currentTimeHundredNanos(), uuid.timestamp(), 1e6);
    }

    @Test
    public void testVersion1NodeBasedTimestamp() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        // Test time fields (within reasonable limits +/- 100 ms or so?)
        assertEquals(UUIDFactory.Clock.currentTimeHundredNanos(), uuid.timestamp(), 1e6);
    }

    @Test
    public void testVersion1NodeBasedUniMulticastBitUnset() {
        // Do it a couple of times, to avoid accidentally have correct bit
        for (int i = 0; i < 100; i++) {
            UUID uuid = UUIDFactory.timeNodeBasedV1();
            assertEquals(0, (uuid.node() >> 40) & 1);
        }
    }

    @Test
    public void testVersion1NodeBasedUnique() {
        for (int i = 0; i < 100; i++) {
            UUID a = UUIDFactory.timeNodeBasedV1();
            UUID b = UUIDFactory.timeNodeBasedV1();
            assertFalse(a.equals(b));
        }
    }

    @Test
    public void testVersion1SecureRandomVariantVersion() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();

        assertEquals(2, uuid.variant());
        assertEquals(1, uuid.version());
    }

    @Test
    public void testVersion1SecureRandomNode() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();
        assertEquals(UUIDFactory.SECURE_RANDOM_NODE, uuid.node());
    }

    @Test
    public void testVersion1SecureRandomFromStringRep() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();
        assertEquals(uuid, UUID.fromString(uuid.toString()));
    }

    @Test
    public void testVersion1SecureRandomClockSeq() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();
        assertEquals(UUIDFactory.Clock.getClockSequence(), uuid.clockSequence());
    }

    @Test
    public void testVersion1SecureRandomTimestamp() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();

        // Test time fields (within reasonable limits +/- 100 ms or so?)
        assertEquals(UUIDFactory.Clock.currentTimeHundredNanos(), uuid.timestamp(), 1e6);
    }

    @Test
    public void testVersion1SecureRandomUniMulticastBit() {
        // Do it a couple of times, to avoid accidentally have correct bit
        for (int i = 0; i < 100; i++) {
            UUID uuid = UUIDFactory.timeRandomBasedV1();
            assertEquals(1, (uuid.node() >> 40) & 1);
        }
    }

    @Test
    public void testVersion1SecureRandomUnique() {
        for (int i = 0; i < 100; i++) {
            UUID a = UUIDFactory.timeRandomBasedV1();
            UUID b = UUIDFactory.timeRandomBasedV1();
            assertFalse(a.equals(b));
        }
    }

    // Clock tests

    @Test(timeout = 10000l)
    public void testClock() throws InterruptedException {
        final long[] times = new long[100000];

        ExecutorService service = Executors.newFixedThreadPool(20);
        for (int i = 0; i < times.length; i++) {
            final int index = i;

            service.submit(new Runnable() {
                public void run() {
                    times[index] = UUIDFactory.Clock.currentTimeHundredNanos();
                }
            });
        }

        service.shutdown();
        assertTrue("Execution timed out", service.awaitTermination(10, TimeUnit.SECONDS));

        Arrays.sort(times);

        for (int i = 0, timesLength = times.length; i < timesLength; i++) {
            if (i == 0) {
                continue;
            }

            assertFalse(String.format("times[%d] == times[%d]: 0x%016x", i - 1, i, times[i]), times[i - 1] == times[i]);
        }
    }

    @Test(timeout = 10000l)
    public void testClockSkew() throws InterruptedException {
        long clockSequence = UUIDFactory.Clock.getClockSequence();

        ExecutorService service = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100000; i++) {
            service.submit(new Runnable() {
                public void run() {
                    UUIDFactory.Clock.currentTimeHundredNanos();
                }
            });
        }

        service.shutdown();
        assertTrue("Execution timed out", service.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(clockSequence, UUIDFactory.Clock.getClockSequence(), 1); // Verify that clock skew doesn't happen "often"
    }

    // Tests for node address system property

    @Test
    public void testParseNodeAddressesSingle() {
        long[] nodes = UUIDFactory.parseMacAddressNodes("00:11:22:33:44:55");

        assertEquals(1, nodes.length);
        assertEquals(0x001122334455l, nodes[0]);
    }

    @Test
    public void testParseNodeAddressesSingleWhitespace() {
        long[] nodes = UUIDFactory.parseMacAddressNodes("  00:11:22:33:44:55\r\n");

        assertEquals(1, nodes.length);
        assertEquals(0x001122334455l, nodes[0]);
    }

    @Test
    public void testParseNodeAddressesMulti() {
        long[] nodes = UUIDFactory.parseMacAddressNodes("00:11:22:33:44:55, aa:bb:cc:dd:ee:ff, \n\t 0a-1b-2c-3d-4e-5f,");

        assertEquals(3, nodes.length);
        assertEquals(0x001122334455l, nodes[0]);
        assertEquals(0xaabbccddeeffl, nodes[1]);
        assertEquals(0x0a1b2c3d4e5fl, nodes[2]);
    }

    @Test(expected = NullPointerException.class)
    public void testParseNodeAddressesNull() {
        UUIDFactory.parseMacAddressNodes(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesEmpty() {
        UUIDFactory.parseMacAddressNodes("");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesNonAddress() {
        UUIDFactory.parseMacAddressNodes("127.0.0.1");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesBadAddress() {
        UUIDFactory.parseMacAddressNodes("00a:11:22:33:44:55");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesBadAddress4() {
        long[] longs = UUIDFactory.parseMacAddressNodes("00:11:22:33:44:550");
        System.err.println("Long: " + Long.toHexString(longs[0]));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesBadAddress2() {
        UUIDFactory.parseMacAddressNodes("0x:11:22:33:44:55");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNodeAddressesBadAddress3() {
        UUIDFactory.parseMacAddressNodes("00:11:22:33:44:55:99");
    }

    // Comparator test

    @Test
    public void testComparator() {
        UUID min = new UUID(0, 0);
        // Long.MAX_VALUE and MIN_VALUE are really adjacent values when comparing unsigned...
        UUID midLow = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
        UUID midHigh = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID max = new UUID(-1l, -1l);

        Comparator<UUID> comparator = UUIDFactory.comparator();

        assertEquals(0, comparator.compare(min, min));
        assertEquals(-1, comparator.compare(min, midLow));
        assertEquals(-1, comparator.compare(min, midHigh));
        assertEquals(-1, comparator.compare(min, max));

        assertEquals(1, comparator.compare(midLow, min));
        assertEquals(0, comparator.compare(midLow, midLow));
        assertEquals(-1, comparator.compare(midLow, midHigh));
        assertEquals(-1, comparator.compare(midLow, max));

        assertEquals(1, comparator.compare(midHigh, min));
        assertEquals(1, comparator.compare(midHigh, midLow));
        assertEquals(0, comparator.compare(midHigh, midHigh));
        assertEquals(-1, comparator.compare(midHigh, max));

        assertEquals(1, comparator.compare(max, min));
        assertEquals(1, comparator.compare(max, midLow));
        assertEquals(1, comparator.compare(max, midHigh));
        assertEquals(0, comparator.compare(max, max));
    }

    @Test
    public void testComparatorRandom() {
        final Comparator<UUID> comparator = UUIDFactory.comparator();

        for (int i = 0; i < 1000; i++) {
            UUID one = UUID.randomUUID();
            UUID two = UUID.randomUUID();

            if (one.getMostSignificantBits() < 0 && two.getMostSignificantBits() >= 0
                    || one.getMostSignificantBits() >= 0 && two.getMostSignificantBits() < 0
                    || one.getLeastSignificantBits() < 0 && two.getLeastSignificantBits() >= 0
                    || one.getLeastSignificantBits() >= 0 && two.getLeastSignificantBits() < 0) {
                // These will differ due to the differing signs
                assertEquals(-one.compareTo(two), comparator.compare(one, two));
            }
            else {
                assertEquals(one.compareTo(two), comparator.compare(one, two));
            }
        }
    }
    
    // Various testing

    @Ignore("Development testing only")
    @Test
    public void testOracleSYS_GUID() {
        // TODO: Consider including this as a "fromCompactString" or similar...
        String str = "AEB87F28E222D08AE043803BD559D08A";
        BigInteger bigInteger = new BigInteger(str, 16); // ALT: Create byte array of every 2 chars.
        long msb = bigInteger.shiftRight(64).longValue();
        long lsb = bigInteger.longValue();
        UUID uuid = new UUID(msb, lsb);
        System.err.println("uuid: " + uuid);
        System.err.println("uuid.variant(): " + uuid.variant());
        System.err.println("uuid.version(): " + uuid.version());
    }
}