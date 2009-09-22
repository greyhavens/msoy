//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.web.gwt.ExternalSiteId;

/**
 * Maps our member accounts to external user ids. Used to integrate with external services like
 * Facebook.
 */
@Entity
public class ExternalMapRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ExternalMapRecord> _R = ExternalMapRecord.class;
    public static final ColumnExp AUTHER = colexp(_R, "auther");
    public static final ColumnExp SITE_ID = colexp(_R, "siteId");
    public static final ColumnExp EXTERNAL_ID = colexp(_R, "externalId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp SESSION_KEY = colexp(_R, "sessionKey");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 5;

    /** The partner that maintains the external user id. */
    @Id public ExternalSiteId.Auther auther;

    /** The id of the site. The interpretation depends on the auther and the implemented support
     * for site variation. (e.g. Whirled Games has a totally different front page to Whirled
     * Rooms.) */
    @Id public int siteId;

    /** The external user identifier. Might be numberic, but we'll store it as a string in case we
     * one day want to support an external site that uses string identifiers. */
    @Id public String externalId;

    /** The id of the Whirled account associated with the specified external account. */
    @Index(name="ixMemberId")
    public int memberId;

    /** The most recent session key provided by the external site, for use in making API requests
     * to said site based on our most recently active session. */
    @Column(nullable=true)
    public String sessionKey;

    /**
     * Returns the site identifier for this mapping.
     */
    public ExternalSiteId getSiteId ()
    {
        return new ExternalSiteId(auther, siteId);
    }

    /**
     * Returns a where clause that matches the specified (non-primary) key.
     */
    public static Where getMemberKey (ExternalSiteId site, int memberId)
    {
        return new Where(AUTHER, site.auther, SITE_ID, site.siteId, MEMBER_ID, memberId);
    }

    /**
     * Gets the primary key for the given site and external user id.
     */
    public static Key<ExternalMapRecord> getKey (ExternalSiteId site, String externalId)
    {
        return getKey(site.auther, site.siteId, externalId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ExternalMapRecord}
     * with the supplied key values.
     */
    public static Key<ExternalMapRecord> getKey (ExternalSiteId.Auther auther, int siteId, String externalId)
    {
        return new Key<ExternalMapRecord>(
                ExternalMapRecord.class,
                new ColumnExp[] { AUTHER, SITE_ID, EXTERNAL_ID },
                new Comparable[] { auther, siteId, externalId });
    }
    // AUTO-GENERATED: METHODS END
}
