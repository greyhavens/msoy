//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Adds or updates the given template.
     */
    public void storeTemplate (FacebookTemplateRecord template)
    {
        store(template);
    }

    /**
     * Deletes the template record with the given code.
     */
    public void deleteTemplate (String code)
    {
        delete(FacebookTemplateRecord.getKey(code));
    }

    /**
     * Loads a list of all saved templates.
     */
    public List<FacebookTemplateRecord> loadTemplates ()
    {
        return findAll(FacebookTemplateRecord.class);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
    }
}
