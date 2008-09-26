//
// $Id$

package com.threerings.msoy.web.data;

/**
 * Utilities used by tracking cookies.
 */
public class TrackingCookieUtil
{
    /** 
     * Trivially simple string obfuscation scheme, via XOR plus a checksum. 
     * 
     * Since GWT JRE library doesn't have any string encoding routines, we roll our own, 
     * but it's easy because HTTP headers can only contain ASCII values. 
     */
    public static byte[] encode (String input)
    {
        int total = 0;
        byte[] bytes = new byte[input.length() + 1];
        char[] chars = input.toCharArray();

        for (int ii = 0; ii < chars.length; ii++) {
            int num = chars[ii];
            if (num < -128 || num > 127) {
                num = '_'; // just in case, although this shouldn't happen
            }
            total += num;
            bytes[ii] = (byte)(num ^ OBFUSCATION_MASK);
        }

        bytes[input.length()] = (byte)(total % 128);
        return bytes;
    }

    /** Trivially simple ASCII decoder. */
    public static String decode (byte[] bytes)
    {
        if (bytes == null) {
            return null;
        }
        int total = 0;
        StringBuilder sb = new StringBuilder();

        for (int ii = 0; ii < bytes.length - 1; ii++) {
            byte fixed = (byte)(bytes[ii] ^ OBFUSCATION_MASK);
            total += fixed;
            sb.append((char) fixed);
        }

        total = (byte)(total % 128);
        byte check = bytes[bytes.length - 1];
        // CShell.log("CRC check: " + total + ", expected: " + check);

        // if the checksum doesn't check out, someone has been tampering with our cookies!
        // let's just return an empty string.
        if (total != check) {
            return null;
        }

        return sb.toString();
    }

    private static final byte OBFUSCATION_MASK = 90; // 01011010
}
