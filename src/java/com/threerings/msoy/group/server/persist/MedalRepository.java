package com.threerings.msoy.group.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the persistent store of Medal data.
 */
@Singleton @BlockingThread
public class MedalRepository extends DepotRepository
{
    @Inject public MedalRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected void getManagedRecords(Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MedalRecord.class);
        classes.add(EarnedMedalRecord.class);
    }
}
