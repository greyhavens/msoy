//
// $Id$

package com.threerings.msoy.item.remix.tools;

/**
 * Constants that should be in JSwiff but aren't.
 */
public class SWFConstants
{
    /** All dimensions inside a SWF are specified in units of "twips". */
    public static final int TWIPS_PER_PIXEL = 20;

    /** Magic crap that's (usually) prepended to JPEG data in a SWF. */
    public static final byte[] JPEG_HEADER = {
        (byte) 0xff, (byte) 0xd9, (byte) 0xff, (byte) 0xd8 };
}
