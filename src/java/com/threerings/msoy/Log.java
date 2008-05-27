//
// $Id$

package com.threerings.msoy;

import com.samskivert.util.Logger;

/**
 * A placeholder class that contains a reference to the log object used by
 * the Msoy package. This is a useful pattern to use when using the
 * samskivert logging facilities. One creates a top-level class like this
 * one that instantiates a log object with an name that identifies log
 * messages from that package and then provides static methods that
 * generate log messages using that instance. Then, classes in that
 * package need only import the log wrapper class and can easily use it to
 * generate log messages. For example:
 *
 * <pre>
 * import com.threerings.msoy.Log;
 * // ...
 * Log.warning("All hell is breaking loose!");
 * // ...
 * </pre>
 */
public class Log
{
    /** The static log instance configured for use by this package. */
    public static Logger log = Logger.getLogger("com.threerings.msoy");
}
