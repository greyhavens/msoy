//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    /** Identifies stock thumbnail images. */
    public static final String THUMBNAIL = "thumbnails";

    /** Identifies stock furniture visualizations. */
    public static final String FURNI = "furni";

    /**
     * Used for unserialization.
     */
    public StaticMediaDesc ()
    {
    }

    /**
     * Creates a configured static media descriptor.
     */
    public StaticMediaDesc (String type, byte itemType)
    {
        _type = type;
        _itemType = itemType;
        mimeType = MediaDesc.IMAGE_PNG;
    }

    /**
     * Returns the type of static media to which this descriptor refers.
     * Currently one of {@link #THUMBNAIL} or {@link #FURNI}.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns the type of item for which we're providing static media.
     */
    public byte getItemType ()
    {
        return _itemType;
    }

    // @Override // from MediaDesc
    public String getMediaPath ()
    {
        return "/media/static/" + _type + "/" +
            Item.getTypeName(_itemType).toLowerCase() + ".png";
    }

    protected String _type;
    protected byte _itemType;
}
