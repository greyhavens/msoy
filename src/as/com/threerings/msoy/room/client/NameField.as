//
// $Id$

package com.threerings.msoy.room.client {

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import com.threerings.util.Log;

import com.threerings.flash.TextFieldUtil;

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

        // It's ok that we modify this later, as it gets cloned anyway when assigned to the field.
        defaultTextFormat = FORMAT;
    }

    /**
     * Sets the color we should use when drawing our name label.
     */
    public function setStatus (status :int, italicize :Boolean = false) :void 
    {
        if (_ignoreStatus) {
            return;
        }

        switch (status) {
        case OccupantInfo.IDLE:
            textColor = 0x777777;
            break;
        case OccupantInfo.DISCONNECTED:
            textColor = 0x80803C;
            break;
        case MsoyBodyObject.AWAY:
            textColor = 0xFFFF77;
            break;
        default:
            textColor = 0x99BFFF;
        }

        // turn on or off italicizing.
        TextFieldUtil.updateFormat(this, { italic: italicize });
    }

//    private static const log :Log = Log.getLog(NameField);

    protected var _ignoreStatus :Boolean;

    protected static const FORMAT :TextFormat =
        TextFieldUtil.createFormat({ font: "_sans", size: 12, bold: true });
}
}
