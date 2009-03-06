//
// $Id$

package com.threerings.msoy.utils {

/**
 * Utility methods for dealing with arguments to msoy web pages.
 */
public class Args
{
    /**
     * Joins the supplies arguments with an underscore between them, escaping unsafe characters.
     */
    public static function join (... args) :String
    {
        var escRE :RegExp = new RegExp(ARG_ESC, "g");
        var sepRE :RegExp = new RegExp(ARG_SEP, "g");
        return args.map(function (element :*, ... rest) :String {
            // TODO: do we need to also encodeURIComponent()?
            return String(element).replace(escRE, ARG_ESC_ESC).replace(sepRE, ARG_ESC_SEP);
        }).join(ARG_SEP);
    }

    protected static const ARG_SEP :String = "_";
    protected static const ARG_ESC :String = "%";
    protected static const ARG_ESC_ESC :String = "%%";
    protected static const ARG_ESC_SEP :String = "%-"; // Note: dash, not underscore!
}
}
