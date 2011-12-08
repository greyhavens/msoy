//
// $Id$

package com.threerings.msoy.apps.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Repository for applications.
 */
@Singleton @BlockingThread
public class AppRepository extends DepotRepository
{
    /**
     * Creates a new repository.
     */
    @Inject public AppRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Loads the info for all defined applications.
     */
    public List<AppInfoRecord> loadApps ()
    {
        return findAll(AppInfoRecord.class, OrderBy.ascending(AppInfoRecord.NAME));
    }

    /**
     * Creates a new application. The unique id field will be assigned upon return.
     */
    public void createApp (AppInfoRecord app)
    {
        insert(app);
    }

    /**
     * Loads the info for the application with the given id.
     */
    public AppInfoRecord loadAppInfo (int appId)
    {
        return load(AppInfoRecord.getKey(appId));
    }

    /**
     * Loads the app info for a given domain name.
     */
    public AppInfoRecord loadAppInfo (String domain)
    {
        return load(AppInfoRecord._R, new Where(AppInfoRecord.DOMAIN.eq(domain)));
    }

    /**
     * Deletes the application with the given id.
     */
    public void deleteApp (int appId)
    {
        delete(AppInfoRecord.getKey(appId));
    }

    /**
     * Updates the given application record, which must already exist.
     */
    public void updateAppInfo (AppInfoRecord app)
    {
        update(app);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(AppInfoRecord.class);
    }
}
