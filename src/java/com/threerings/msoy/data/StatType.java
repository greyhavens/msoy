//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;
import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.MaxValueIntStat;
import com.threerings.stats.data.Stat;

/**
 * Enumerates the various stats used in Whirled.
 */
@ActionScript(omit=true)
public enum StatType implements Stat.Type
{
    // social stats
    FRIENDS_MADE(new IntStat(), true),
    INVITES_ACCEPTED(new IntStat(), true),
    WHIRLED_COMMENTS(new IntStat(), true),
    MINUTES_ACTIVE(new IntStat(), true),
    CONSEC_DAILY_LOGINS(new MaxValueIntStat(), true),
    WHIRLEDS_VISITED(new IntSetStat(), true),
    WHIRLEDS_CREATED(new IntStat(), true),

    // game stats
    TROPHIES_EARNED(new IntStat(), true),
    GAME_SESSIONS(new IntStat(), true),
    UNIQUE_GAMES_PLAYED(new IntSetStat(), true),
    MP_GAMES_WON(new IntStat(), true),
    MP_GAME_PARTNERS(new IntSetStat(), true),

    // creation stats
    PHOTOS_LISTED(new IntStat(), true),
    PHOTOS_SOLD(new IntStat(), true),
    PHOTOS_PURCHASED(new IntStat(), true),

    DOCUMENTS_LISTED(new IntStat(), true),
    DOCUMENTS_SOLD(new IntStat(), true),
    DOCUMENTS_PURCHASED(new IntStat(), true),

    FURNITURES_LISTED(new IntStat(), true),
    FURNITURES_SOLD(new IntStat(), true),
    FURNITURES_PURCHASED(new IntStat(), true),

    GAMES_LISTED(new IntStat(), true),
    GAMES_SOLD(new IntStat(), true),
    GAMES_PURCHASED(new IntStat(), true),

    AVATARS_LISTED(new IntStat(), true),
    AVATARS_SOLD(new IntStat(), true),
    AVATARS_PURCHASED(new IntStat(), true),

    PETS_LISTED(new IntStat(), true),
    PETS_SOLD(new IntStat(), true),
    PETS_PURCHASED(new IntStat(), true),

    AUDIOS_LISTED(new IntStat(), true),
    AUDIOS_SOLD(new IntStat(), true),
    AUDIOS_PURCHASED(new IntStat(), true),

    VIDEOS_LISTED(new IntStat(), true),
    VIDEOS_SOLD(new IntStat(), true),
    VIDEOS_PURCHASED(new IntStat(), true),

    DECORS_LISTED(new IntStat(), true),
    DECORS_SOLD(new IntStat(), true),
    DECORS_PURCHASED(new IntStat(), true),

    TOYS_LISTED(new IntStat(), true),
    TOYS_SOLD(new IntStat(), true),
    TOYS_PURCHASED(new IntStat(), true),

    UNUSED(new IntStat());

    public static StatType getItemsListedStat (byte itemType)
    {
        return _itemsListedStats.get(itemType);
    }

    public static StatType getItemsSoldStat (byte itemType)
    {
        return _itemsSoldStats.get(itemType);
    }

    public static StatType getItemsPurchasedStat (byte itemType)
    {
        return _itemsPurchasedStats.get(itemType);
    }

    /** Returns the translation key used by this stat. */
    public String key ()
    {
        return MessageBundle.qualify(
            MsoyCodes.STATS_MSGS, "m.stat_" + StringUtil.toUSLowerCase(name()));
    }

    // from interface Stat.Type
    public Stat newStat ()
    {
        return (Stat)_prototype.clone();
    }

    // from interface Stat.Type
    public int code ()
    {
        return _code;
    }

    // from interface Stat.Type
    public boolean isPersistent ()
    {
        return _persist;
    }

    // most stats are persistent
    StatType (Stat prototype)
    {
        this(prototype, true);
    }

    StatType (Stat prototype, boolean persist)
    {
        _persist = persist;
        _prototype = prototype;

        // configure our prototype and map ourselves into the Stat system
        _code = Stat.initType(this, _prototype);
    }

    protected Stat _prototype;
    protected int _code;
    protected boolean _persist;

    protected static HashIntMap<StatType> _itemsListedStats;
    protected static HashIntMap<StatType> _itemsSoldStats;
    protected static HashIntMap<StatType> _itemsPurchasedStats;

    static
    {
        _itemsListedStats = new HashIntMap<StatType>();
        _itemsSoldStats = new HashIntMap<StatType>();
        _itemsPurchasedStats = new HashIntMap<StatType>();

        _itemsListedStats.put    (Item.PHOTO, StatType.PHOTOS_LISTED);
        _itemsSoldStats.put      (Item.PHOTO, StatType.PHOTOS_SOLD);
        _itemsPurchasedStats.put (Item.PHOTO, StatType.PHOTOS_PURCHASED);
        _itemsListedStats.put    (Item.DOCUMENT, StatType.DOCUMENTS_LISTED);
        _itemsSoldStats.put      (Item.DOCUMENT, StatType.DOCUMENTS_SOLD);
        _itemsPurchasedStats.put (Item.DOCUMENT, StatType.DOCUMENTS_PURCHASED);
        _itemsListedStats.put    (Item.FURNITURE, StatType.FURNITURES_LISTED);
        _itemsSoldStats.put      (Item.FURNITURE, StatType.FURNITURES_SOLD);
        _itemsPurchasedStats.put (Item.FURNITURE, StatType.FURNITURES_PURCHASED);
        _itemsListedStats.put    (Item.GAME, StatType.GAMES_LISTED);
        _itemsSoldStats.put      (Item.GAME, StatType.GAMES_SOLD);
        _itemsPurchasedStats.put (Item.GAME, StatType.GAMES_PURCHASED);
        _itemsListedStats.put    (Item.AVATAR, StatType.AVATARS_LISTED);
        _itemsSoldStats.put      (Item.AVATAR, StatType.AVATARS_SOLD);
        _itemsPurchasedStats.put (Item.AVATAR, StatType.AVATARS_PURCHASED);
        _itemsListedStats.put    (Item.PET, StatType.PETS_LISTED);
        _itemsSoldStats.put      (Item.PET, StatType.PETS_SOLD);
        _itemsPurchasedStats.put (Item.PET, StatType.PETS_PURCHASED);
        _itemsListedStats.put    (Item.AUDIO, StatType.AUDIOS_LISTED);
        _itemsSoldStats.put      (Item.AUDIO, StatType.AUDIOS_SOLD);
        _itemsPurchasedStats.put (Item.AUDIO, StatType.AUDIOS_PURCHASED);
        _itemsListedStats.put    (Item.VIDEO, StatType.VIDEOS_LISTED);
        _itemsSoldStats.put      (Item.VIDEO, StatType.VIDEOS_SOLD);
        _itemsPurchasedStats.put (Item.VIDEO, StatType.VIDEOS_PURCHASED);
        _itemsListedStats.put    (Item.DECOR, StatType.DECORS_LISTED);
        _itemsSoldStats.put      (Item.DECOR, StatType.DECORS_SOLD);
        _itemsPurchasedStats.put (Item.DECOR, StatType.DECORS_PURCHASED);
        _itemsListedStats.put    (Item.TOY, StatType.TOYS_LISTED);
        _itemsSoldStats.put      (Item.TOY, StatType.TOYS_SOLD);
        _itemsPurchasedStats.put (Item.TOY, StatType.TOYS_PURCHASED);
    }
}
