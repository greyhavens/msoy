//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;

import com.whirled.data.ItemInfo;
import com.whirled.data.LevelInfo;
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
    public static final String WHIRLED_GAME_SERVICE = "whirledGameService";

    /** The field name of the <code>levelPacks</code> field. */
    public static final String LEVEL_PACKS = "levelPacks";

    /** The field name of the <code>itemPacks</code> field. */
    public static final String ITEM_PACKS = "itemPacks";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public WhirledGameMarshaller whirledGameService;

    /** The set of level packs available to this game. */
    public LevelInfo[] levelPacks;

    /** The set of item packs available to this game. */
    public ItemInfo[] itemPacks;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>whirledGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWhirledGameService (WhirledGameMarshaller value)
    {
        WhirledGameMarshaller ovalue = this.whirledGameService;
        requestAttributeChange(
            WHIRLED_GAME_SERVICE, value, ovalue);
        this.whirledGameService = value;
    }

    /**
     * Requests that the <code>levelPacks</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLevelPacks (LevelInfo[] value)
    {
        LevelInfo[] ovalue = this.levelPacks;
        requestAttributeChange(
            LEVEL_PACKS, value, ovalue);
        this.levelPacks = (value == null) ? null : (LevelInfo[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>levelPacks</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setLevelPacksAt (LevelInfo value, int index)
    {
        LevelInfo ovalue = this.levelPacks[index];
        requestElementUpdate(
            LEVEL_PACKS, index, value, ovalue);
        this.levelPacks[index] = value;
    }

    /**
     * Requests that the <code>itemPacks</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setItemPacks (ItemInfo[] value)
    {
        ItemInfo[] ovalue = this.itemPacks;
        requestAttributeChange(
            ITEM_PACKS, value, ovalue);
        this.itemPacks = (value == null) ? null : (ItemInfo[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>itemPacks</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setItemPacksAt (ItemInfo value, int index)
    {
        ItemInfo ovalue = this.itemPacks[index];
        requestElementUpdate(
            ITEM_PACKS, index, value, ovalue);
        this.itemPacks[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
