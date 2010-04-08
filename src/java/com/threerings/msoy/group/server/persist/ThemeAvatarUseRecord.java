//
// $Id: $

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/** Tracks the most recent avatar used by a player in a themed room. */
@Entity
public class ThemeAvatarUseRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeAvatarUseRecord> _R = ThemeAvatarUseRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp LAST_WORN = colexp(_R, "lastWorn");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the member who wore the avatar. */
    @Id
    public int memberId;

    /** The groupId of the theme for which the avatar was worn. */
    @Id
    public int groupId;

    /** The item id of the avatar the player most recently wore. */
    public int itemId;

    /** When the avatar was last worn. */
    public Timestamp lastWorn;

    public ThemeAvatarUseRecord ()
    {
    }

    public ThemeAvatarUseRecord (int memberId, int groupId, int itemId)
    {
        this.memberId = memberId;
        this.groupId = groupId;
        this.itemId = itemId;
        this.lastWorn = new Timestamp(System.currentTimeMillis());
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThemeAvatarUseRecord}
     * with the supplied key values.
     */
    public static Key<ThemeAvatarUseRecord> getKey (int memberId, int groupId)
    {
        return newKey(_R, memberId, groupId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, GROUP_ID); }
    // AUTO-GENERATED: METHODS END
}
