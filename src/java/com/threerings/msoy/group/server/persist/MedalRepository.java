//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Logic.And;

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

    /**
     * If the medal's medalId is valid, it will update that medalId's row in the database.
     * Otherwise, it will insert a new row.
     */
    public boolean storeMedal (MedalRecord medal)
    {
        return store(medal);
    }

    /**
     * Returns true if the groupId and name combination is already in use.
     */
    public boolean groupContainsMedalName (int groupId, String name)
    {
        Equals groupIdEquals = new Equals(MedalRecord.GROUP_ID_C, groupId);
        Equals nameEquals = new Equals(MedalRecord.NAME_C, name);
        return load(MedalRecord.class, new Where(new And(groupIdEquals, nameEquals))) != null;
    }

    @Override
    protected void getManagedRecords(Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MedalRecord.class);
        classes.add(EarnedMedalRecord.class);
    }
}
