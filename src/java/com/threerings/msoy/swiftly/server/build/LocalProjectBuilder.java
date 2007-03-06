//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
/**
 * Server-local project builder.
 */
public class LocalProjectBuilder
    implements ProjectBuilder
{
    public LocalProjectBuilder (SwiftlyProjectRecord record, ProjectStorage storage)
    {
        _projectRecord = record;
        _storage = storage;
    }

    public void build ()
        throws ProjectBuilderException
    {
        // Do something
    }

    /** Reference to our project record. */
    protected SwiftlyProjectRecord _projectRecord;

    /** Reference to our backing project storage. */
    protected ProjectStorage _storage;
}
