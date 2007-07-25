//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of decor (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(name="itemId", pkColumnValue="DECOR")
public class DecorRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(DecorRecord.class, TYPE);

    /** The column identifier for the {@link #height} field. */
    public static final String HEIGHT = "height";

    /** The qualified column identifier for the {@link #height} field. */
    public static final ColumnExp HEIGHT_C =
        new ColumnExp(DecorRecord.class, HEIGHT);

    /** The column identifier for the {@link #width} field. */
    public static final String WIDTH = "width";

    /** The qualified column identifier for the {@link #width} field. */
    public static final ColumnExp WIDTH_C =
        new ColumnExp(DecorRecord.class, WIDTH);

    /** The column identifier for the {@link #depth} field. */
    public static final String DEPTH = "depth";

    /** The qualified column identifier for the {@link #depth} field. */
    public static final ColumnExp DEPTH_C =
        new ColumnExp(DecorRecord.class, DEPTH);

    /** The column identifier for the {@link #horizon} field. */
    public static final String HORIZON = "horizon";

    /** The qualified column identifier for the {@link #horizon} field. */
    public static final ColumnExp HORIZON_C =
        new ColumnExp(DecorRecord.class, HORIZON);

    /** The column identifier for the {@link #hideWalls} field. */
    public static final String HIDE_WALLS = "hideWalls";

    /** The qualified column identifier for the {@link #hideWalls} field. */
    public static final ColumnExp HIDE_WALLS_C =
        new ColumnExp(DecorRecord.class, HIDE_WALLS);

    /** The column identifier for the {@link #offsetX} field. */
    public static final String OFFSET_X = "offsetX";

    /** The qualified column identifier for the {@link #offsetX} field. */
    public static final ColumnExp OFFSET_X_C =
        new ColumnExp(DecorRecord.class, OFFSET_X);

    /** The column identifier for the {@link #offsetY} field. */
    public static final String OFFSET_Y = "offsetY";

    /** The qualified column identifier for the {@link #offsetY} field. */
    public static final ColumnExp OFFSET_Y_C =
        new ColumnExp(DecorRecord.class, OFFSET_Y);
    // AUTO-GENERATED: FIELDS END

    /** Room type. Controls how the background wallpaper image is handled. */
    public byte type;
    
    /** Room height, in pixels. */
    public short height;
    
    /** Room width, in pixels. */
    public short width;
    
    /** Room depth, in pixels. */
    public short depth;
    
    /** Horizon position, in [0, 1]. */
    public float horizon;

    /** Specifies whether side walls should be displayed. */
    public boolean hideWalls;

    /** Bitmap offset along the x axis, in room units. */
    public float offsetX;

    /** Bitmap offset along the y axis, in room units. */
    public float offsetY;

    public static final int SCHEMA_VERSION = 2 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    public DecorRecord ()
    {
        super();
    }

    protected DecorRecord (Decor decor)
    {
        super(decor);

        type = decor.type;
        height = decor.height;
        width = decor.width;
        depth = decor.depth;
        horizon = decor.horizon;
        hideWalls = decor.hideWalls;
        offsetX = decor.offsetX;
        offsetY = decor.offsetY;
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.DECOR;
    }

    @Override
    protected Item createItem ()
    {
        Decor object = new Decor();
        object.type = type;
        object.height = height;
        object.width = width;
        object.depth = depth;
        object.horizon = horizon;
        object.hideWalls = hideWalls;
        object.offsetX = offsetX;
        object.offsetY = offsetY;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DecorRecord}
     * with the supplied key values.
     */
    public static Key<DecorRecord> getKey (int itemId)
    {
        return new Key<DecorRecord>(
                DecorRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
