package com.twelvemonkeys.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class UUIDFactoryTest {

    // Nil UUID

    @Test
    public void testNilUUID() {
        UUID nil = UUIDFactory.NIL;
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID b = new UUID(0l, 0l);

        assertEquals(nil, b);
        assertEquals(nil, a);
        assertEquals(a, b);

        assertEquals(0, nil.variant());
        assertEquals(0, nil.version());
    }

    // Version 3 UUIDs (for comparison with v5)

    @Test
    public void testVersion3NameBasedMD5() throws UnsupportedEncodingException {
        String name  = "http://www.example.com/uuid/";
        UUID a = UUID.nameUUIDFromBytes(name.getBytes("UTF-8"));
        assertEquals(3, a.version());
        assertEquals(2, a.variant());

        UUID b = UUID.nameUUIDFromBytes(name.getBytes("UTF-8"));
        assertEquals(a, b);

        assertFalse(a.equals(UUIDFactory.nameUUIDFromBytesSHA1(name.getBytes("UTF-8"))));

        assertEquals(a, UUID.fromString(a.toString()));
    }

    // Version 5 UUIDs

    @Test
    public void testVersion5NameBasedSHA1() throws UnsupportedEncodingException {
        String name  = "http://www.example.com/uuid/";
        UUID a = UUIDFactory.nameUUIDFromBytesSHA1(name.getBytes("UTF-8"));
        assertEquals(5, a.version());
        assertEquals(2, a.variant());

        UUID b = UUIDFactory.nameUUIDFromBytesSHA1(name.getBytes("UTF-8"));
        assertEquals(a, b);

        assertFalse(a.equals(UUID.nameUUIDFromBytes(name.getBytes("UTF-8"))));
        assertEquals(a, UUID.fromString(a.toString()));
    }

    // Version 1 UUIDs

    @Test
    public void testVersion1NodeBased() {
        UUID uuid = UUIDFactory.timeNodeBasedV1();
        System.err.println("uuid: " + uuid);

        assertEquals(1, uuid.version());
        assertEquals(2, uuid.variant());

        assertEquals(UUIDFactory.MAC_ADDRESS_NODE, uuid.node());
        // TODO: Test that this is actually a Mac address from the local computer, or specified through system property

        assertEquals(uuid, UUID.fromString(uuid.toString()));

        assertEquals(UUIDFactory.Clock.getClockSequence(), uuid.clockSequence());
        // Test time fields (within reasonable limits +/- 100 ms or so?)
        // TODO: Compare with system clock for sloppier resolution
        assertEquals(UUIDFactory.Clock.currentTimeHundredNanos(), uuid.timestamp(), 1e6);

        assertEquals(0, (uuid.node() >> 40) & 1);
    }

    @Test
    public void testVersion1NodeBasedUnique() {
        UUID a = UUIDFactory.timeNodeBasedV1();
        UUID b = UUIDFactory.timeNodeBasedV1();

        System.err.println("a: " + a);
        System.err.println("b: " + b);

        assertFalse(a.equals(b));
    }

    @Test
    public void testVersion1SecureRandom() {
        UUID uuid = UUIDFactory.timeRandomBasedV1();
        System.err.println("uuid: " + uuid);

        assertEquals(1, uuid.version());
        assertEquals(2, uuid.variant());

        assertEquals(UUIDFactory.SECURE_RANDOM_NODE, uuid.node());

        assertEquals(uuid, UUID.fromString(uuid.toString()));

        assertEquals(UUIDFactory.Clock.getClockSequence(), uuid.clockSequence());

        // TODO: Test time fields (within reasonable limits +/- 100 ms or so?)
        assertEquals(UUIDFactory.Clock.currentTimeHundredNanos(), uuid.timestamp(), 1e6);

        assertEquals(1, (uuid.node() >> 40) & 1);
    }

    @Test
    public void testVersion1SecureRandomUnique() {
        UUID a = UUIDFactory.timeRandomBasedV1();
        UUID b = UUIDFactory.timeRandomBasedV1();

        System.err.println("a: " + a);
        System.err.println("b: " + b);

        assertFalse(a.equals(b));
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

    // Various testing

    @Test
    public void testOracleSYS_GUID() {
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