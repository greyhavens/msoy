//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.net.URLVariables;

import com.threerings.util.ParameterUtil;

/**
 * A small utility class for handling parameters in whirled.
 */
public class MsoyParameters
{
    /**
     * Return the parameters in use on this msoy client.
     */
    public static function get () :Object
    {
        return _params;
    }

    /**
     * Initialize.
     */
    public static function init (disp :DisplayObject, thenRun :Function) :void
    {
        var d :DisplayObject = disp;
        while (d != null) {
            try {
                var s :String = Object(d).getWhirledParams();
                _params = new URLVariables(s);
                thenRun();
                return;
            } catch (err :Error) {
                // fall through
            }
            try {
                d = d.parent;
            } catch (err :Error) {
                d = null;
            }
        }

        ParameterUtil.getParameters(disp, function (params :Object) :void {
            _params = params;
            thenRun();
        });
    }

    /** The parameters. */
    protected static var _params :Object;
}
}
