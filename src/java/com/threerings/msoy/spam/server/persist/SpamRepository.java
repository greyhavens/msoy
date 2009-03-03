//
// $Id$

package com.threerings.msoy.spam.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Coordinates persistence for data related to external emailing of members.
 */
@Singleton @BlockingThread
public class SpamRepository extends DepotRepository
{
    @Inject SpamRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SpamRecord.class);
    }
}
