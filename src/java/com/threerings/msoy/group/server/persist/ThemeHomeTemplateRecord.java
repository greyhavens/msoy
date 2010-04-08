//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/** Enumerates the home room templates available to a new player in a theme. */
@Entity
public class ThemeHomeTemplateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeHomeTemplateRecord> _R = ThemeHomeTemplateRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp SCENE_ID = colexp(_R, "sceneId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The groupId of the theme for which we're enumerating home room templates. */
    @Id
    public int groupId;

    /** The sceneId of the template room. */
    @Id
    public int sceneId;

    public ThemeHomeTemplateRecord ()
    {
    }

    public ThemeHomeTemplateRecord (int groupId, int sceneId)
    {
        this.groupId = groupId;
        this.sceneId = sceneId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThemeHomeTemplateRecord}
     * with the supplied key values.
     */
    public static Key<ThemeHomeTemplateRecord> getKey (int groupId, int sceneId)
    {
        return newKey(_R, groupId, sceneId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(GROUP_ID, SCENE_ID); }
    // AUTO-GENERATED: METHODS END
}
