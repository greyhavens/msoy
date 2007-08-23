//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.ezgame.data.EZGameObject;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;

/**
 * Maintains additional state for MSOY games.
 */
public class MsoyGameObject extends EZGameObject
    implements WhirledGame
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>whirledGameService</code> field. */
    public static const WHIRLED_GAME_SERVICE :String = "whirledGameService";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public var whirledGameService :WhirledGameMarshaller;

    // from interface WhirledGame
    public function getWhirledGameService () :WhirledGameMarshaller
    {
        return whirledGameService;
    }

    override protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        super.readDefaultFields(ins);

        whirledGameService = (ins.readObject() as WhirledGameMarshaller);
    }
}
}
