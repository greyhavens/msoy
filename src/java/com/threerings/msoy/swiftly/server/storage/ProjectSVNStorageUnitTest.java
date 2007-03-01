//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.List;

import junit.framework.TestCase;

public class ProjectSVNStorageUnitTest extends TestCase
{
    public ProjectSVNStorageUnitTest (String name)
    {
        super(name);
    }

    /** Set up the test case anew. */
    public void setUp ()
        throws Exception
    {
        File svnDir;
        
        // Create a temporary directory.
        _tempDir = File.createTempFile("svnstorage", "test");
        _tempDir.delete();
        if (_tempDir.mkdir() != true) {
            throw new Exception("Temporary directory '" + _tempDir + "' already exists!");
        }

        // Mock up a project record.
        _projectRecord = new SwiftlyProjectRecord();
        _projectRecord.projectName = "project-name";
        _projectRecord.ownerId = 0;
        
        _storageRecord = new SwiftlySVNStorageRecord();
        _storageRecord.protocol = ProjectSVNStorage.PROTOCOL_FILE;
        _storageRecord.baseDir = _tempDir.getAbsolutePath();

        // Initialize the storage
        ProjectSVNStorage.initializeStorage(_projectRecord, _storageRecord, TEMPLATE_DIR.getCanonicalFile());
    }


    /** Clean up afterwards. */
    public void tearDown ()
        throws Exception
    {
        FileUtils.deleteDirectory(_tempDir);
    }

    /** Try opening of a project. */
    public void testOpenStorage ()
        throws Exception
    {
        ProjectStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
    }


    public void testGetProjectSVNURL ()
        throws Exception
    {
        ProjectSVNStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
        assertEquals("file://" + _tempDir + "/" + _projectRecord.projectId,
            storage.getSVNURL().toString());
    }

    /** Try listing a project. */
    public void testGetProjectTree ()
        throws Exception
    {
        ProjectStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
        List<PathElement> projectTree = storage.getProjectTree();
        assertTrue("The returned PathElement list is empty", projectTree.size() != 0);

        // Simple sanity check of one of the paths, to ensure that it matches the template
        boolean foundPNGFile = false;
        boolean foundASFile = false;
        
        for (PathElement node : projectTree) {
            if (node.getName().equals("test.png")) {
                foundPNGFile = true;
                PathElement parent = node.getParent();
                assertEquals("media", parent.getName());
                assertEquals(PathElement.Type.ROOT, parent.getParent().getType());
                assertEquals("image/png", node.getMimeType());
            }
            if (node.getName().equals("UnitTest.as")) {
                foundASFile = true;
                PathElement parent = node.getParent();
                assertEquals(PathElement.Type.ROOT, parent.getType());
                assertEquals("text/x-actionscript", node.getMimeType());
            }
        }
        assertTrue("The returned pathElement list does not contain the expected test.png file",
            foundPNGFile == true);
        assertTrue("The returned pathElement list does not contain the expected UnitTest.as file",
            foundASFile == true);

    }


    /** Temporary test directory. */
    protected File _tempDir;

    /** Mocked up project record. */
    protected SwiftlyProjectRecord _projectRecord;
    
    /** Mocked up storage record. */
    protected SwiftlySVNStorageRecord _storageRecord;

    /** Static, brittle path to the test template. Sorry. */
    static final File TEMPLATE_DIR = new File("data/swiftly/templates/unittest");
}
