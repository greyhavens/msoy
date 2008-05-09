//
// $Id$

package com.threerings.msoy.world.client {

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Log;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MsoyBodyObject;

/**
 * A class to encapsulate the formatting used in occupant name text fields.
 */
public class NameField extends TextField
{
    /**
     * @param ignoreStatus If true, keeps the name white, regardless of the status that gets 
     *                     passed in.
     */
    public function NameField (ignoreStatus :Boolean = false)
    {
        _ignoreStatus = ignoreStatus;

        textColor = 0xFFFFFF; // set up default color
        selectable = false;
        autoSize = TextFieldAutoSize.CENTER;
        filters = [ new GlowFilter(0, 1, 2, 2, 255) ];

        var format :TextFormat = new TextFormat();
        // there be magic here. Arial isn't even available on Linux, but it works it out. The
        // documentation for TextFormat does not indicate this. Bastards.
        format.font = "Arial";
        format.size = 12;
        format.bold = true;
        defaultTextFormat = format;
    }

    /**
     * Sets the color we should use when drawing our name label.
     */
    public function setStatus (status :int, italic :Boolean = false) :void 
    {
        if (_ignoreStatus) {
            return;
        }

        switch (status) {
        case OccupantInfo.IDLE:
            textColor = 0x777777;
            break;
        case OccupantInfo.DISCONNECTED:
            textColor = 0xFF0000;
            break;
        case MsoyBodyObject.AWAY:
            textColor = 0xFFFF77;
            break;
        default:
            textColor = 0x99BFFF;
        }

        defaultTextFormat.italic = italic;
    }

//    private static const log :Log = Log.getLog(NameField);

    protected var _ignoreStatus :Boolean;
}
}
