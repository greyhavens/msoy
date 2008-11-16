//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.LiteralExp;

import com.threerings.presents.annotation.BlockingThread;

import static com.threerings.msoy.Log.log;

/**
 * Manages the AffiliateMapRecords.
 */
@Singleton @BlockingThread
public class AffiliateMapRepository extends DepotRepository
{
    @Inject public AffiliateMapRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP: blow away bogus affiliate mappings
        registerMigration(new DataMigration("2008_09_25_clearAffiliateMappings") {
            public void invoke () throws DatabaseException
            {
                deleteAll(AffiliateMapRecord.class, new Where(new LiteralExp("true")));
            }
        });
    }

    /**
     * Get the memberId associated with the specified affiliate, but create a new
     * mapping if one is not currently present.
     */
    public int getAffiliateMemberId (String affiliate)
    {
        AffiliateMapRecord rec = load(AffiliateMapRecord.class, affiliate);
        if (rec == null) {
            // try parsing the affiliate as a memberId.
            try {
                int memberId = Integer.parseInt(affiliate);
                // oh hey, that parsed. We do not presently create a mapping record
                // for the String -> int, but in the future we may allow such mappings to be
                // made to reassign affiliations to a different memberId.
                if (memberId > 0) {
                    return memberId;
                }
            } catch (NumberFormatException nfe) {
                // that didn't work, fall through and create a new record...
            }

            // store a new blank mapping from this affiliate to 0
            rec = new AffiliateMapRecord();
            rec.affiliate = affiliate;
            try {
                insert(rec);
            } catch (DatabaseException e) {
                // try to load again...
                AffiliateMapRecord newRec = load(AffiliateMapRecord.class, affiliate);
                if (newRec != null) {
                    rec = newRec;

                } else {
                    log.warning("Trouble inserting new affiliate", "affiliate", affiliate, e);
                }
            }
        }

        return rec.memberId;
    }

    /**
     * Get the specified page of mappings.
     */
    public List<AffiliateMapRecord> getMappings (int start, int count)
    {
        return findAll(AffiliateMapRecord.class,
            OrderBy.ascending(AffiliateMapRecord.AFFILIATE_C), new Limit(start, count));
    }

    /**
     * Get the total number of mappings in the database.
     */
    public int getMappingCount ()
    {
        return load(CountRecord.class, new FromOverride(AffiliateMapRecord.class)).count;
    }

    /**
     * Update or insert a new mapping.
     * Note: this should be done in conjunction with
     * {@link MemberRepository#updateAffiliateMemberId}.
     */
    public void storeMapping (String affiliate, int memberId)
    {
        AffiliateMapRecord rec = new AffiliateMapRecord();
        rec.affiliate = affiliate;
        rec.memberId = memberId;
        store(rec);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(AffiliateMapRecord.class);
    }
}
