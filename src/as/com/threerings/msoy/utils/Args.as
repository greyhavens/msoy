//
// $Id$

package com.threerings.msoy.utils {

/**
 * Useful static methods for handling msoy web page arguments.
 */
public class Args
{
    /**
     * Joins the supplied arguments with an underscore between each as a string.
     */
    public static function join (...args) :String
    {
        var joined :String = "";
        if (args.length > 0) {
            joined = args[0];
            args = args.slice(1);
        }
        for each (var arg :String in args) {
            joined += "_" + arg;
        }
        return joined;
    }
}
}
