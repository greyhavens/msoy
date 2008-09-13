//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DatabaseException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.DuplicateKeyException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.PersistenceContext.CacheTraverser; 
import com.samskivert.jdbc.depot.PersistentRecord;

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
    }

    /**
     * Get the memberId associated with the specified affiliate, but create a new
     * mapping if one is not currently present.
     */
    public int getAffiliateMemberId (String affiliate)
    {
        AffiliateMapRecord rec = load(AffiliateMapRecord.class, affiliate);
        if (rec == null) {
            rec = new AffiliateMapRecord();
            rec.affiliate = affiliate;
            try {
                insert(rec);
            } catch (DatabaseException e) {
                // try to load again...
                AffiliateMapRecord newRec = load(AffiliateMapRecord.class, affiliate);
                if (newRec == null) {
                    log.warning("Trouble inserting new affiliate", "affiliate", affiliate);
                } else {
                    rec = newRec;
                }
            }
        }

        return rec.memberId;
    }

    /**
     * Update or insert a new mapping.
     */
    public void storeMapping (String affiliate, int memberId)
    {
        AffiliateMapRecord rec = new AffiliateMapRecord();
        rec.affiliate = affiliate;
        rec.memberId = memberId;
        store(rec);
    }

    /**
     * Oh god, load all the current mappings. TODO: pagination, someday, brah.
     */
    public Map<String, Integer> getAll ()
    {
        Map<String, Integer> map = Maps.newHashMap();
        for (AffiliateMapRecord rec : findAll(AffiliateMapRecord.class)) {
            map.put(rec.affiliate, rec.memberId);
        }
        return map;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(AffiliateMapRecord.class);
    }
}
