//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/** Clone records for Avatars. */
@Entity
@Table
@TableGenerator(name="cloneId", allocationSize=-1,
                initialValue=-1, pkColumnValue="AVATAR_CLONE")
public class AvatarCloneRecord extends CloneRecord<AvatarRecord>
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #scale} field. */
    public static final String SCALE = "scale";

    /** The qualified column identifier for the {@link #scale} field. */
    public static final ColumnExp SCALE_C =
        new ColumnExp(AvatarCloneRecord.class, SCALE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The scale to apply to the avatar. */
    @Column(defaultValue="1")
    public float scale;
}
