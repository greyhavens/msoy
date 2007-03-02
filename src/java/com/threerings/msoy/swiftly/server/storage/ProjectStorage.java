//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;

import java.util.List;

/**
 * Defines the project storage interface
 */
public interface ProjectStorage
{    
    /** Returns a list of path elements that compose the entirity of the project tree. */
    public List<PathElement> getProjectTree () throws ProjectStorageException;

    /** Loads and returns a SwiftlyDocument from cold storage. */
    public SwiftlyDocument getDocument (String path) throws ProjectStorageException;
}
