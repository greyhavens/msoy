//
// $Id$

package com.threerings.msoy.item.server.persist;

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
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="DECOR")
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

    public static final int SCHEMA_VERSION = 1 +
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
        return object;
    }
}
