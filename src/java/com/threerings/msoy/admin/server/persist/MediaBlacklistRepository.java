//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Maintains the persistent media blacklist.
 */
@Singleton @BlockingThread
public class MediaBlacklistRepository extends DepotRepository
{
    @Inject public MediaBlacklistRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Test to see if the given media descriptor is blacklisted.
     */
    public boolean isBlacklisted (MediaDesc desc)
    {
        return null != load(MediaBlacklistRecord.class, new Where(
            MediaBlacklistRecord.MEDIA_HASH.eq(Exps.value(desc.hash))));
    }

    public void blacklist (MediaDesc desc, String note)
    {
        insert(new MediaBlacklistRecord(desc, note));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MediaBlacklistRecord.class);
    }
}
