//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRepository;

import java.util.ArrayList;

/**
 * Handles the collection of Swiftly project information
 */
public class SwiftlyManager
{
    /**
     * Configures us with our repository.
     */
    public void init (SwiftlyProjectRepository srepo)
    {
        _srepo = srepo;
    }


    /**
     * Retrieve a list of projects to which a user has commit privileges.
     */
    public ArrayList<SwiftlyProjectRecord> findProjects (MemberRecord mrec)
    {
        try {
            return _srepo.findProjects(mrec.memberId);
        } catch (PersistenceException e) {
            // TODO: Error handling
        }

        return null;
    }

    /**
     * Create a new project.
     */
    public void createProject (MemberRecord mrec, String projectName)
        throws PersistenceException
    {
        // TODO: Maximum number of projects?
        SwiftlyProjectRecord record = new SwiftlyProjectRecord();
        record.projectName = projectName;
        record.ownerId = mrec.memberId;
        _srepo.createProject(record);
    }

    /** Handles persistent stuff. */
    protected SwiftlyProjectRepository _srepo;
}
