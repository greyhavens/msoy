//
// $Id$

package com.threerings.msoy.item.data.all {

/**
 * Collection of static constants and functions related to the type of an item.
 */
public class ItemTypes
{
    // WARNING: DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    /** The type constant for an Occupant. */
    public static const OCCUPANT :int = -1;

    /** The type constant for an unassigned or invalid item. */
    public static const NOT_A_TYPE :int = 0;

    /** The type constant for a {@link Photo} item. */
    public static const PHOTO :int = 1;

    /** The type constant for a {@link Document} item. */
    public static const DOCUMENT :int = 2;

    /** The type constant for a {@link Furniture} item. */
    public static const FURNITURE :int = 3;

    /** The type constant for a {@link Game} item. */
    public static const GAME :int = 4;

    // WARNING: DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    /** The type constant for a {@link Avatar} item. */
    public static const AVATAR :int = 5;

    /** The type constant for a {@link Pet} item. */
    public static const PET :int = 6;

    /** The type constant for a {@link Audio} item. */
    public static const AUDIO :int = 7;

    /** The type constant for a {@link Video} item. */
    public static const VIDEO :int = 8;

    /** The type constant for a {@link Decor} item. */
    public static const DECOR :int = 9;

    /** The type constant for a {@link Toy} item. */
    public static const TOY :int = 10;

    // WARNING: DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    /** The type constant for a {@link LevelPack} item. */
    public static const LEVEL_PACK :int = 11;

    /** The type constant for a {@link ItemPack} item. */
    public static const ITEM_PACK :int = 12;

    /** The type constant for a {@link TrophySource} item. */
    public static const TROPHY_SOURCE :int = 13;

    /** The type constant for a {@link Prize} item. */
    public static const PRIZE :int = 14;

    /** The type constant for a {@link Prop} item. */
    public static const PROP :int = 15;

    // WARNING: DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    /**
     * Get the Stringy name of the specified item type.
     */
    public static function getTypeName (type :int) :String
    {
        // We can't use a switch statement because our final variables are not actually constants
        // (they are assigned values at class initialization time).
        if (type == PHOTO) {
            return "photo"; 
        } else if (type == AVATAR) {
            return "avatar";
        } else if (type == GAME) {
            return "game";
        } else if (type == FURNITURE) {
            return "furniture";
        } else if (type == DOCUMENT) { 
            return "document";
        } else if (type == PET) { 
            return "pet";
        } else if (type == AUDIO) { 
            return "audio";
        } else if (type == VIDEO) {
            return "video";
        } else if (type == DECOR) {
            return "decor";
        } else if (type == TOY) {
            return "toy";
        } else if (type == LEVEL_PACK) {
            return "level_pack";
        } else if (type == ITEM_PACK) {
            return "item_pack";
        } else if (type == TROPHY_SOURCE) {
            return "trophy_source";
        } else if (type == PRIZE) {
            return "prize";
        } else if (type == PROP) {
            return "prop";
        } else {
            return "unknown:" + type;
        }
    }

}

}
