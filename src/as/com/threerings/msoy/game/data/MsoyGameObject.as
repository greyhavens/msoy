//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.TypedArray;

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

    /** The field name of the <code>levelPacks</code> field. */
    public static const LEVEL_PACKS :String = "levelPacks";

    /** The field name of the <code>itemPacks</code> field. */
    public static const ITEM_PACKS :String = "itemPacks";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public var whirledGameService :WhirledGameMarshaller;

    /** The set of level packs available to this game. */
    public var levelPacks :TypedArray /*LevelInfo*/;

    /** The set of item packs available to this game. */
    public var itemPacks :TypedArray /*ItemInfo*/;

    // from interface WhirledGame
    public function getWhirledGameService () :WhirledGameMarshaller
    {
        return whirledGameService;
    }

    override protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        super.readDefaultFields(ins);

        whirledGameService = (ins.readObject() as WhirledGameMarshaller);
        levelPacks = (ins.readObject() as TypedArray);
        itemPacks = (ins.readObject() as TypedArray);
    }
}
}
