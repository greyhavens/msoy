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

    public static function setSource (source :String) :void
    {
        // force new parameters
        _params = new URLVariables(source);
    }

    public static function init (disp :DisplayObject) :void
    {
        ParameterUtil.getParameters(disp, gotParameters);
    }

    protected static function gotParameters (params :Object) :void
    {
        // do not overwrite any params we already have
        if (_params == null) {
            _params = params;
        }
    }

    /** The parameters. */
    protected static var _params :Object;
}
}
