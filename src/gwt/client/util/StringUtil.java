//
// $Id$

package client.util;

/** 
 * Useful string utilities, brought over to GWT land from {@link com.samskivert.util.StringUtil}. 
 */
public class StringUtil
{
    /**
     * Generates a string from the supplied bytes that is the HEX encoded representation of those
     * bytes.  Returns the empty string for a <code>null</code> or empty byte array.
     *
     * @param bytes the bytes for which we want a string representation.
     * @param count the number of bytes to stop at (which will be coerced into being <= the length
     * of the array).
     */
    public static String hexlate (byte[] bytes, int count)
    {
        if (bytes == null) {
            return "";
        }

        count = Math.min(count, bytes.length);
        char[] chars = new char[count*2];

        for (int i = 0; i < count; i++) {
            int val = bytes[i];
            if (val < 0) {
                val += 256;
            }
            chars[2*i] = XLATE.charAt(val/16);
            chars[2*i+1] = XLATE.charAt(val%16);
        }

        return new String(chars);
    }

    /**
     * Generates a string from the supplied bytes that is the HEX encoded representation of those
     * bytes.
     */
    public static String hexlate (byte[] bytes)
    {
        return (bytes == null) ? "" : hexlate(bytes, bytes.length);
    }

    /**
     * Turn a hexlated String back into a byte array.
     */
    public static byte[] unhexlate (String hex)
    {
        if (hex == null || (hex.length() % 2 != 0)) {
            return null;
        }

        // if for some reason we are given a hex string that wasn't made by hexlate, convert to
        // lowercase so things work.
        hex = hex.toLowerCase();
        byte[] data = new byte[hex.length()/2];
        for (int ii = 0; ii < hex.length(); ii+=2) {
            int value = (byte)(XLATE.indexOf(hex.charAt(ii)) << 4);
            value  += XLATE.indexOf(hex.charAt(ii+1));
            // values over 127 are wrapped around, restoring negative bytes
            data[ii/2] = (byte)value;
        }

        return data;
    }

    /**
     * Returns true if the supplied string is null or zero length.
     */
    public static boolean isBlank (String text)
    {
        return (text == null) || (text.length() == 0);
    }

    /**
     * Returns the string if it is non-blank (see {@link #isBlank}), the default value otherwise.
     */
    public static String getOr (String value, String defval)
    {
        return isBlank(value) ? defval : value;
    }

    /**
     * Escapes user or deployment values that we need to put into an html attribute.
     */
    public static String escapeAttribute (String value)
    {
        for (int ii = 0; ii < ATTR_ESCAPES.length; ++ii) {
            value = value.replace(ATTR_ESCAPES[ii][0], ATTR_ESCAPES[ii][1]);
        }
        return value;
    }

    /**
     * Nukes special attribute characters. Ideally this would not be needed, but some integrations
     * do not accept special characters in attributes.
     */
    public static String sanitizeAttribute (String value)
    {
        for (int ii = 0; ii < ATTR_ESCAPES.length; ++ii) {
            value = value.replace(ATTR_ESCAPES[ii][0], "");
        }
        return value;
    }

    /** Map of strings that must be replaced inside html attributes and their replacements. (They
     * need to be applied in order so amps are not double escaped.) */
    protected static final String[][] ATTR_ESCAPES = {
        {"&", "&amp;"},
        {"'", "&apos;"},
        {"\"", "&quot;"},
        {"<", "&lt;"},
        {">", "&gt;"},
    };

    /** Used by {@link #hexlate} and {@link #unhexlate}. */
    protected static final String XLATE = "0123456789abcdef";
}
