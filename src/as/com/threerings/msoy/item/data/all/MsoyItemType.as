package com.threerings.msoy.item.data.all
{
import com.threerings.util.ByteEnum;

public class MsoyItemType extends ByteEnum
{
    public static const OCCUPANT :MsoyItemType = new MsoyItemType("OCCUPANT", -1);
    public static const NOT_A_TYPE :MsoyItemType = new MsoyItemType("NOT_A_TYPE", 0);
    public static const PHOTO  :MsoyItemType = new MsoyItemType("PHOTO", 1);
    public static const DOCUMENT  :MsoyItemType = new MsoyItemType("DOCUMENT", 2);
    public static const FURNITURE  :MsoyItemType = new MsoyItemType("FURNITURE", 3);
    public static const _DEPRECATED_GAME  :MsoyItemType = new MsoyItemType("_DEPRECATED_GAME", 4);
    public static const AVATAR  :MsoyItemType = new MsoyItemType("AVATAR", 5);
    public static const PET  :MsoyItemType = new MsoyItemType("PET", 6);
    public static const AUDIO  :MsoyItemType = new MsoyItemType("AUDIO", 7);
    public static const VIDEO  :MsoyItemType = new MsoyItemType("VIDEO", 8);
    public static const DECOR  :MsoyItemType = new MsoyItemType("DECOR", 9);
    public static const TOY  :MsoyItemType = new MsoyItemType("TOY", 10);
    public static const LEVEL_PACK  :MsoyItemType = new MsoyItemType("LEVEL_PACK", 11);
    public static const ITEM_PACK  :MsoyItemType = new MsoyItemType("ITEM_PACK", 12);
    public static const TROPHY_SOURCE  :MsoyItemType = new MsoyItemType("TROPHY_SOURCE", 13);
    public static const PRIZE  :MsoyItemType = new MsoyItemType("PRIZE", 14);
    public static const PROP  :MsoyItemType = new MsoyItemType("PROP", 15);
    public static const LAUNCHER  :MsoyItemType = new MsoyItemType("LAUNCHER", 16);

    public function MsoyItemType (name :String, code :int)
    {
        super(name, code);
    }
}
}
