//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTest;

/**
 * Maintains persistent data for a/b tests
 */
@Singleton @BlockingThread
public class ABTestRepository extends DepotRepository
{
    @Inject public ABTestRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all test information with newest tests first
     */
    public List<ABTestRecord> loadTests ()
        throws PersistenceException
    {
        return findAll(ABTestRecord.class, OrderBy.descending(ABTestRecord.AB_TEST_ID_C));
    }

    /**
     * Loads all tests that are currently enabled
     */
    public List<ABTestRecord> loadRunningTests ()
        throws PersistenceException
    {
        return findAll(ABTestRecord.class, new Where(ABTestRecord.ENABLED_C, true));
    }

    /**
     * Loads a single test by the unique string identifier (name)
     */
    public ABTestRecord loadTestByName (String name)
        throws PersistenceException
    {
        return load(ABTestRecord.class, new Where(ABTestRecord.NAME_C, name));
    }

    /**
     * Inserts the supplied record into the database.
     */
    public void insertABTest (ABTest test)
        throws PersistenceException
    {
        try {
            ABTestRecord record = ABTestRecord.class.newInstance();
            record.fromABTest(test);
            insert(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the supplied record in the database.
     */
    public void updateABTest (ABTest test)
        throws PersistenceException
    {
        try {
            ABTestRecord record = ABTestRecord.class.newInstance();
            record.fromABTest(test);
            update(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ABTestRecord.class);
    }
}
