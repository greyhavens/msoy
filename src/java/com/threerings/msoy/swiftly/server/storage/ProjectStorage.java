//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;

import java.io.File;
import java.util.List;

/**
 * Defines the project storage interface
 */
public interface ProjectStorage
{    
    /** Returns a list of path elements that compose the entirety of the project tree. */
    public List<PathElement> getProjectTree () throws ProjectStorageException;

    /** Loads and returns a SwiftlyDocument from cold storage. */
    public SwiftlyDocument getDocument (PathElement path) throws ProjectStorageException;

    /** Store the given document, creating it if it does not already exist. */
    public void putDocument (SwiftlyDocument document, String logMessage)
        throws ProjectStorageException;

    /** Rename the given path. */
    public void renameDocument (PathElement pathElement, String newName, String logMessage)
        throws ProjectStorageException;

    /** Delete the given path. */
    public void deleteDocument (PathElement pathElement, String logMessage)
        throws ProjectStorageException;

    /**
     * Exports the entire project repository to a given file system path. All project files
     * will be created relative to the provided path. */
    public void export (File exportDirectory) throws ProjectStorageException;

    /** The standard text encoding. Changing this WILL break existing projects. So don't. */
    public static final String TEXT_ENCODING = "utf8";
}
