//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorageUnitTest;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.web.data.SwiftlyProject;

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
        _project = ProjectSVNStorageUnitTest.mockProject();
        storageRecord = ProjectSVNStorageUnitTest.mockStorageRecord(_tempDir);

        // Initialize the storage
        _storage = ProjectSVNStorage.initializeStorage(_project, storageRecord,
            ProjectSVNStorageUnitTest.GAME_TEMPLATE_DIR.getCanonicalFile());
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
        ProjectBuilder builder = new LocalProjectBuilder(_project, _storage,
            FLEX_SDK_DIR.getAbsoluteFile(), WHIRLED_SDK_DIR.getAbsoluteFile(),
            SERVER_ROOT.getAbsoluteFile());
        BuildResult result = builder.build(_tempDir, new MemberName());
        for (CompilerOutput output : result.getOutput()) {
            // no output should be displayed if the build worked
            assertEquals("", output);
        }
        assertTrue(result.buildSuccessful());
        // TODO: test broken build output
    }

    /** Temporary test directory. */
    protected File _tempDir;

    /** Mocked up project record. */
    protected SwiftlyProject _project;

    /** Project storage. */
    protected ProjectStorage _storage;

    /** Static, brittle path to the flex SDK. Sorry. */
    public static final File FLEX_SDK_DIR = new File("data/swiftly/flex_sdk");

    /** Static, brittle path to the whirled SDK. Sorry. */
    public static final File WHIRLED_SDK_DIR = new File("data/swiftly/whirled_sdk");

    /** Static, brittle path to the server root. Sorry. */
    public static final File SERVER_ROOT = new File("");

}
