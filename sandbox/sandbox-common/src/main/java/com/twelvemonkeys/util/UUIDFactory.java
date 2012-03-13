/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;

import com.twelvemonkeys.lang.StringUtil;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * A factory for creating {@code UUID}s not directly supported by {@link java.util.UUID java.util.UUID}.
 * <p>
 * This class can create version 1 time based {@code UUID}s, using either IEEE 802 (mac) address or random "node" value
 * as well as version 5 SHA1 hash based {@code UUID}s.
 * </p>
 * <p>
 * The timestamp value for version 1 {@code UUID}s will use a high precision clock, when available to the Java VM.
 * If the Java system clock does not offer the needed precision, the timestamps will fall back to 100-nanosecond
 * increments, to avoid duplicates.
 * </p>
 * <p>
 * <a name="mac-node"></a>
 * The node value for version 1 {@code UUID}s will, by default, reflect the IEEE 802 (mac) address of one of
 * the network interfaces of the local computer.
 * This node value can be manually overridden by setting
 * the system property {@code "com.twelvemonkeys.util.UUID.node"} to a valid IEEE 802 address, on the form
 * {@code 12:34:56:78:9a:bc} or {@code 12-34-45-78-9a-bc}.
 * </p>
 * <p>
 * <a name="random-node"></a>
 * The node value for the random "node" based version 1 {@code UUID}s will be stable for the lifetime of the VM.
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: UUIDFactory.java,v 1.0 27.02.12 09:45 haraldk Exp$
 *
 * @see <a href="http://tools.ietf.org/html/rfc4122">RFC 4122</a>
 * @see <a href="http://en.wikipedia.org/wiki/Universally_unique_identifier">Wikipedia</a>
 * @see java.util.UUID
 */
public final class UUIDFactory {
    private static final String NODE_PROPERTY = "com.twelvemonkeys.util.UUID.node";
    
    /**
     * The Nil UUID: {@code "00000000-0000-0000-0000-000000000000"}.
     *
     * The nil UUID is special form of UUID that is specified to have all
     * 128 bits set to zero. Not particularly useful, unless as a placeholder or template.
     *
     * @see <a href="http://tools.ietf.org/html/rfc4122#section-4.1.7">RFC 4122 4.1.7. Nil UUID</a>
     */
    public static final UUID NIL = new UUID(0l, 0l);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Comparator<UUID> COMPARATOR = new UUIDComparator();

    // Assumes MAC address is constant, which it may not be if a network card is replaced
    static final long MAC_ADDRESS_NODE = getMacAddressNode();

    static final long SECURE_RANDOM_NODE = getSecureRandomNode();

    private static long getSecureRandomNode() {
        // Creates a completely random "node" value, with the unicast bit set to 1, as outlined in RFC 4122.
        return 1l << 40 | SECURE_RANDOM.nextLong() & 0xffffffffffffl;
    }

    private static long getMacAddressNode() {
        long[] addressNodes;

        String nodeProperty = System.getProperty(NODE_PROPERTY);

        // Read mac address/node from system property, to allow user-specified node addresses.
        if (!StringUtil.isEmpty(nodeProperty)) {
            addressNodes = parseMacAddressNodes(nodeProperty);
        }
        else {
            addressNodes = MacAddressFinder.getMacAddressNodes();
        }

        // TODO: The UUID spec allows us to use multiple nodes, when available, to create more UUIDs per time unit...
        // For example in a round robin fashion?
        return addressNodes != null && addressNodes.length > 0 ? addressNodes[0] : -1;
    }

    static long[] parseMacAddressNodes(final String nodeProperty) {
        // Parse comma-separated list mac addresses on format 00:11:22:33:44:55 / 00-11-22-33-44-55
        String[] nodesStrings = nodeProperty.trim().split(",\\W*");
        long[] addressNodes = new long[nodesStrings.length];

        for (int i = 0, nodesStringsLength = nodesStrings.length; i < nodesStringsLength; i++) {
            String nodesString = nodesStrings[i];

            try {
                String[] nodes = nodesString.split("(?<=(^|\\W)[0-9a-fA-F]{2})\\W(?=[0-9a-fA-F]{2}(\\W|$))", 6);

                long nodeAddress = 0;

                // Network byte order
                nodeAddress |= (long) (Integer.parseInt(nodes[0], 16) & 0xff) << 40;
                nodeAddress |= (long) (Integer.parseInt(nodes[1], 16) & 0xff) << 32;
                nodeAddress |= (long) (Integer.parseInt(nodes[2], 16) & 0xff) << 24;
                nodeAddress |= (long) (Integer.parseInt(nodes[3], 16) & 0xff) << 16;
                nodeAddress |= (long) (Integer.parseInt(nodes[4], 16) & 0xff) << 8;
                nodeAddress |= (long) (Integer.parseInt(nodes[5], 16) & 0xff);

                addressNodes[i] = nodeAddress;
            }
            catch (RuntimeException e) {
                // May be NumberformatException from parseInt or ArrayIndexOutOfBounds from nodes array
                NumberFormatException formatException = new NumberFormatException(String.format("Bad IEEE 802 node address: '%s' (from system property %s)", nodesString, NODE_PROPERTY));
                formatException.initCause(e);
                throw formatException;
            }
        }

        return addressNodes;
    }

    private UUIDFactory() {}

    /**
     * Creates a type 5 (name based) {@code UUID} based on the specified byte array.
     * This method is effectively identical to {@link UUID#nameUUIDFromBytes}, except that this method
     * uses a SHA1 hash instead of the MD5 hash used in the type 3 {@code UUID}s.
     * RFC 4122 states that "SHA-1 is preferred" over MD5, without giving a reason for why.
     *
     * @param name a byte array to be used to construct a {@code UUID}
     * @return  a {@code UUID} generated from the specified array.
     *
     * @see <a href="http://tools.ietf.org/html/rfc4122#section-4.3">RFC 4122 4.3. Algorithm for Creating a Name-Based UUID</a>
     * @see <a href="http://tools.ietf.org/html/rfc4122#appendix-A">RFC 4122 appendix A</a>
     * @see UUID#nameUUIDFromBytes(byte[])
     */
    public static UUID nameUUIDv5FromBytes(byte[] name) {
        // Based on code from OpenJDK UUID#nameUUIDFromBytes + private byte[] constructor
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("SHA1 not supported");
        }

        byte[] sha1Bytes = md.digest(name);
        sha1Bytes[6]  &= 0x0f;  /* clear version        */
        sha1Bytes[6]  |= 0x50;  /* set to version 5     */
        sha1Bytes[8]  &= 0x3f;  /* clear variant        */
        sha1Bytes[8]  |= 0x80;  /* set to IETF variant  */

        long msb = 0;
        long lsb = 0;

        // NOTE: According to RFC 4122, only first 16 bytes are used, meaning
        // bytes 17-20 in the 160 bit SHA1 hash are simply discarded in this case...
        for (int i=0; i<8; i++) {
            msb = (msb << 8) | (sha1Bytes[i] & 0xff);
        }
        for (int i=8; i<16; i++) {
            lsb = (lsb << 8) | (sha1Bytes[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    /**
     * Creates a version 1 time (and node) based {@code UUID}.
     * The node part is by default the IEE 802 (mac) address of one of the network cards of the current machine.
     *
     * @return a {@code UUID} based on the current time and the node address of this computer.
     * @see <a href="http://tools.ietf.org/html/rfc4122#section-4.2">RFC 4122 4.2. Algorithms for Creating a Time-Based UUID</a>
     * @see <a href="http://en.wikipedia.org/wiki/MAC_address">IEEE 802 (mac) address</a>
     * @see <a href="#mac-node">Overriding the node address</a>
     *
     * @throws IllegalStateException if the IEEE 802 (mac) address of the computer (node) cannot be found.
     */
    public static UUID timeNodeBasedUUID() {
        if (MAC_ADDRESS_NODE == -1) {
            throw new IllegalStateException("Could not determine IEEE 802 (mac) address for node");
        }

        return new UUID(createTimeAndVersion(), createClockSeqAndNode(MAC_ADDRESS_NODE));
    }

    /**
     * Creates a version 1 time based {@code UUID} with the node part replaced by a random based value.
     * The node part is computed using a 47 bit secure random number + lsb of first octet (unicast/multicast bit) set to 1.
     * These {@code UUID}s can never clash with "real" node based version 1 {@code UUID}s due to the difference in
     * the unicast/multicast bit, however, no uniqueness between multiple machines/vms/nodes can be guaranteed.
     *
     * @return a {@code UUID} based on the current time and a random generated "node" value.
     * @see <a href="http://tools.ietf.org/html/rfc4122#section-4.5">RFC 4122 4.5. Node IDs that Do Not Identify the Host</a>
     * @see <a href="http://tools.ietf.org/html/rfc4122#appendix-A">RFC 4122 Appendix A</a>
     * @see <a href="#random-node">Lifetime of random node value</a>
     *
     * @throws IllegalStateException if the IEEE 802 (mac) address of the computer (node) cannot be found.
     */
    public static UUID timeRandomBasedUUID() {
        return new UUID(createTimeAndVersion(), createClockSeqAndNode(SECURE_RANDOM_NODE));
    }

    // TODO: Version 2 UUID?
    /*
    Version 2 UUIDs are similar to Version 1 UUIDs, with the upper byte of the clock sequence replaced by the
    identifier for a "local domain" (typically either the "POSIX UID domain" or the "POSIX GID domain")
    and the first 4 bytes of the timestamp replaced by the user's POSIX UID or GID (with the "local domain"
    identifier indicating which it is).[2][3]
     */

    private static long createClockSeqAndNode(final long node) {
        // Variant (2) + Clock seq high and low + node
        return 0x8000000000000000l | (Clock.getClockSequence() << 48) | node & 0xffffffffffffl;
    }

    private static long createTimeAndVersion() {
        long clockTime = Clock.currentTimeHundredNanos();

        long time = clockTime << 32;                    // Time low
        time |= (clockTime & 0xFFFF00000000L) >> 16;    // Time mid
        time |= ((clockTime >> 48) & 0x0FFF);           // Time high
        time |= 0x1000;                                 // Version (1)

        return time;
    }

    /**
     * Returns a comparator that compares UUIDs as 128 bit unsigned entities, as mentioned in RFC 4122.
     * This is different than {@link UUID#compareTo(Object)} that compares the UUIDs as signed entities.
     *
     * @return a comparator that compares UUIDs as 128 bit unsigned entities.
     *
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7025832">java.lang.UUID compareTo() does not do an unsigned compare</a>
     * @see <a href="http://tools.ietf.org/html/rfc4122#appendix-A">RFC 4122 Appendix A</a>
     */
    public static Comparator<UUID> comparator() {
        return COMPARATOR;
    }

    static final class UUIDComparator implements Comparator<UUID> {
        public int compare(UUID left, UUID right) {
            // The ordering is intentionally set up so that the UUIDs
            // can simply be numerically compared as two *UNSIGNED* numbers
            if (left.getMostSignificantBits() >>> 32 < right.getMostSignificantBits() >>> 32) {
                return -1;
            }
            else if (left.getMostSignificantBits() >>> 32 > right.getMostSignificantBits() >>> 32) {
                return 1;
            }
            else if ((left.getMostSignificantBits() & 0xffffffffl) < (right.getMostSignificantBits() & 0xffffffffl)) {
                return -1;
            }
            else if ((left.getMostSignificantBits() & 0xffffffffl) > (right.getMostSignificantBits() & 0xffffffffl)) {
                return 1;
            }
            else if (left.getLeastSignificantBits() >>> 32 < right.getLeastSignificantBits() >>> 32) {
                return -1;
            }
            else if (left.getLeastSignificantBits() >>> 32 > right.getLeastSignificantBits() >>> 32) {
                return 1;
            }
            else if ((left.getLeastSignificantBits() & 0xffffffffl) < (right.getLeastSignificantBits() & 0xffffffffl)) {
                return -1;
            }
            else if ((left.getLeastSignificantBits() & 0xffffffffl) > (right.getLeastSignificantBits() & 0xffffffffl)) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * A high-resolution timer for use in creating version 1 {@code UUID}s.
     */
    static final class Clock {
        // Java: 0:00, Jan. 1st, 1970 vs UUID: 0:00, Oct 15th, 1582
        private static final long JAVA_EPOCH_OFFSET_HUNDRED_NANOS = 122192928000000000L;

        private static int clockSeq = SECURE_RANDOM.nextInt();

        private static long initialNanos;
        private static long initialTime;

        private static long lastMeasuredTime;
        private static long lastTime;

        static {
            initClock();
        }

        private static void initClock() {
            long millis = System.currentTimeMillis();
            long nanos = System.nanoTime();

            initialTime = JAVA_EPOCH_OFFSET_HUNDRED_NANOS + millis * 10000 + (nanos / 100) % 10000;
            initialNanos = nanos;
        }

        public static synchronized long currentTimeHundredNanos() {
            // Measure delta since init and compute accurate time
            long time;

            while ((time = initialTime + (System.nanoTime() - initialNanos) / 100) < lastMeasuredTime) {
                // Reset clock seq (should happen VERY rarely)
                initClock();
                clockSeq++;
            }

            lastMeasuredTime = time;

            if (time <= lastTime) {
                // This typically means the clock isn't accurate enough, use auto-incremented time.
                // It is possible that more timestamps than available are requested for
                // each time unit in the system clock, but that is extremely unlikely.
                // TODO: RFC 4122 says we should wait in the case of too many requests...
                time = ++lastTime;
            }
            else {
                lastTime = time;
            }
            
            return time;
        }

        public static synchronized long getClockSequence() {
            return clockSeq & 0x3fff;
        }
    }

    /**
     * Static inner class for 1.5 compatibility.
     */
    static final class MacAddressFinder {
        public static long[] getMacAddressNodes() {
            List<Long> nodeAddresses = new ArrayList<Long>();
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

                if (interfaces != null) {
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface nic = interfaces.nextElement();

                        if (!nic.isVirtual()) {
                            long nodeAddress = 0;

                            byte[] hardware = nic.getHardwareAddress(); // 1.6 method

                            if (hardware != null && hardware.length == 6 && hardware[1] != (byte) 0xff) {
                                // Network byte order
                                nodeAddress |= (long) (hardware[0] & 0xff) << 40;
                                nodeAddress |= (long) (hardware[1] & 0xff) << 32;
                                nodeAddress |= (long) (hardware[2] & 0xff) << 24;
                                nodeAddress |= (long) (hardware[3] & 0xff) << 16;
                                nodeAddress |= (long) (hardware[4] & 0xff) << 8;
                                nodeAddress |= (long) (hardware[5] & 0xff);

                                nodeAddresses.add(nodeAddress);
                            }
                        }
                    }
                }
            }
            catch (SocketException ex) {
                return null;
            }

            long[] unwrapped = new long[nodeAddresses.size()];
            for (int i = 0, nodeAddressesSize = nodeAddresses.size(); i < nodeAddressesSize; i++) {
                unwrapped[i] = nodeAddresses.get(i);
            }

            return unwrapped;
        }
    }
}
