//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;

/**
 * Describes a story template entered into the Facebook template editor for use at runtime by the
 * msoy server.
 */
@Entity
public class FacebookTemplateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookTemplateRecord> _R = FacebookTemplateRecord.class;
    public static final ColumnExp CODE = colexp(_R, "code");
    public static final ColumnExp BUNDLE_ID = colexp(_R, "bundleId");
    public static final ColumnExp WILDCARDS = colexp(_R, "wildcards");
    // AUTO-GENERATED: FIELDS END

    /** Determines compatible schema versions. */
    public static final int SCHEMA_VERSION = 1;

    /** Unique name. */
    @Id public String code;

    /** Story id, passed to facebook when creating a new story. */
    public long bundleId;

    /**
     * Creates a new template to be filled in with data from the database.
     */
    public FacebookTemplateRecord ()
    {
    }

    /**
     * Creates a new template matching the given runtime template.
     */
    public FacebookTemplateRecord (FacebookTemplate template)
    {
        code = template.code;
        bundleId = template.bundleId;
    }

    /**
     * Creates and returns a runtime template matching this one.
     */
    public FacebookTemplate toTemplate ()
    {
        FacebookTemplate template = new FacebookTemplate();
        template.code = code;
        template.bundleId = bundleId;
        return template;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookTemplateRecord}
     * with the supplied key values.
     */
    public static Key<FacebookTemplateRecord> getKey (String code)
    {
        return new Key<FacebookTemplateRecord>(
                FacebookTemplateRecord.class,
                new ColumnExp[] { CODE },
                new Comparable[] { code });
    }
    // AUTO-GENERATED: METHODS END
}
