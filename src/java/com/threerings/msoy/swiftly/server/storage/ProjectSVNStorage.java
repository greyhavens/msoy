//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.data.PathElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
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

    /*
     * Static initialization of the SVNKit library
     */
    static {
        // Initialize SVNKit repository protocol factories

        // http:// and https://
        DAVRepositoryFactory.setup();

        // svn:// and svn+...://
        SVNRepositoryFactoryImpl.setup();

        // file:///
        FSRepositoryFactory.setup();
    }

    /**
     * Initialize project storage for a given project record.
     */
    public static ProjectSVNStorage initializeStorage (SwiftlyProjectRecord record)
        throws ProjectStorageException
    {
        // TODO: Project Template Here.
        return null;
    }

    /** 
     * Construct a new storage instance for the given project record.
     */
    public ProjectSVNStorage (SwiftlyProjectRecord record)
        throws ProjectStorageException
    {
        // Initialize subversion magic
        try {
            SVNURL url = SVNURL.parseURIEncoded(record.projectSubversionURL);
            _repo = SVNRepositoryFactory.create(url);
            // TODO -- Remote authentication
            // _svnRepo.setAuthenticationManager(auth manager implementation here);
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Could not parse subversion URL");
        }
    }


    // from interface ProjectStorage
    public List<PathElement> getProjectTree ()
        throws ProjectStorageException
    {
        // Recurse over the entirity of the subversion repository, building a list
        // of project path elements.
        // TODO: Extend the Swiftly client to support readdir()-style functionality instead of this sillyness.

        // Stat the project root.
        try {
            SVNNodeKind nodeKind = _repo.checkPath("", -1);

            // Are we starting at a directory?
            if (nodeKind != SVNNodeKind.DIR) {
                throw new ProjectStorageException.InternalError("The subversion root path is not a directory");
            }
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Unable to stat() the project root path: " + e, e);
        }

        try {
            return recurseTree(null, null);
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("A subversion failure occured while" +
                " recursing over the directory tree: " + e, e);
        }

    }


    /** 
     * Recursively retrieves the entire subversion directory structure.
     */
    @SuppressWarnings("unchecked")
    private List<PathElement> recurseTree (PathElement parent, List<PathElement> result)
        throws SVNException, ProjectStorageException
    {
        Collection<SVNDirEntry> entries;
        String path;

        if (result == null) {
            result = new ArrayList<PathElement>();
        }

        if (parent == null) {
            path = "";
        } else {
            path = parent.getAbsolutePath();
        }

        entries = (Collection<SVNDirEntry>) _repo.getDir(path, -1, null, (Collection)null);
        for (SVNDirEntry entry : entries) {
            PathElement node;
            SVNNodeKind kind;
            
            kind = entry.getKind();
            if (kind == SVNNodeKind.DIR) {
                node = PathElement.createDirectory(entry.getName(), parent);
                // Recurse
                recurseTree(node, result);
            } else if (kind == SVNNodeKind.FILE) {
                node = PathElement.createFile(entry.getName(), parent);
            } else {
                throw new ProjectStorageException.InternalError("Received an unhandled subversion node type: " + kind);  
            }

            result.add(node);
        }

        return result;
    }

    /** Reference to the project's subversion repository. */
    private SVNRepository _repo;
}
