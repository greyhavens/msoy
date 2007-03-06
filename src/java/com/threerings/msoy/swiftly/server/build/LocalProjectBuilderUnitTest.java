//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorageUnitTest;

import org.apache.commons.io.FileUtils;

import java.io.File;

import junit.framework.TestCase;

public class LocalProjectBuilderUnitTest extends TestCase
{
    public LocalProjectBuilderUnitTest (String name)
    {
        super(name);
    }


    /** Set up the test case anew. */
    public void setUp ()
        throws Exception
    {
        /** Mocked up storage record. */
        SwiftlySVNStorageRecord storageRecord;

        // Create a temporary directory.
        _tempDir = File.createTempFile("localbuilder", "test");
        _tempDir.delete();
        if (_tempDir.mkdir() != true) {
            throw new Exception("Temporary directory '" + _tempDir + "' already exists!");
        }

        // Mock up a project record.
        _projectRecord = ProjectSVNStorageUnitTest.mockProjectRecord();
        storageRecord = ProjectSVNStorageUnitTest.mockStorageRecord(_tempDir);

        // Initialize the storage
        _storage = ProjectSVNStorage.initializeStorage(_projectRecord, storageRecord,
            ProjectSVNStorageUnitTest.TEMPLATE_DIR.getCanonicalFile());
    }


    /** Clean up afterwards. */
    public void tearDown ()
        throws Exception
    {
        FileUtils.deleteDirectory(_tempDir);
    }

    public void testBuild ()
        throws Exception
    {
        ProjectBuilder builder = new LocalProjectBuilder(_projectRecord, _storage);
        builder.build();
    }

    /** Temporary test directory. */
    protected File _tempDir;

    /** Mocked up project record. */
    protected SwiftlyProjectRecord _projectRecord;

    /** Project storage. */
    protected ProjectStorage _storage;
}
