//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.data.PathElement;


import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;

import static com.threerings.msoy.Log.log;

/**
 * Handles the subversion-based project repository.
 */
public class ProjectSVNStorage
    implements ProjectStorage
{

    static {
        // Initialize SVNKit repository protocol factories

        // http:// and https://
        DAVRepositoryFactory.setup();

        // svn:// and svn+...://
        SVNRepositoryFactoryImpl.setup();

        // file:///
        FSRepositoryFactory.setup();
    }

    public ProjectSVNStorage (SwiftlyProjectRecord record)
        throws ProjectStorageException
    {
        // Initialize subversion magic
        try {
            SVNURL url = SVNURL.parseURIEncoded(record.projectSubversionURL);
            _svnRepo = SVNRepositoryFactory.create(url);
            // TODO -- Remote authentication
            // _svnRepo.setAuthenticationManager(auth manager implementation here);
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Could not parse subversion URL");
        }
    }

    /** Reference to the project's subversion repository. */
    protected SVNRepository _svnRepo;
}
