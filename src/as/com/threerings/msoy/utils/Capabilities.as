/**
 * Simplified access functions to flash.system.Capabilities.
 */

package com.threerings.msoy.utils {

import flash.system.Capabilities;

public class Capabilities {
    /**
     * Get the flash player version as an array of Strings, like [ "9", "0", "115", "0" ].
     */
    public static function getFlashVersion () :Array
    {
        // the version looks like "LNX 9,0,31,0"
        var bits :Array = flash.system.Capabilities.version.split(" ");
        return (bits[1] as String).split(",");
    }

    /**
     * Get the major flash player version as an integer, e.g. 9.
     */
    public static function getFlashMajorVersion () :int
    {
        return int(getFlashVersion()[0]);
    }

    /**
     * Determine if the current Flash player is at least of major version 10. */
    public static function isFlash10 () :Boolean
    {
        return getFlashMajorVersion() >= 10;
    }
}
}
