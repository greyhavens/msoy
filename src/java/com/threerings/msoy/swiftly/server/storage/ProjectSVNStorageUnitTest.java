//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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


    public void testGetDocument ()
        throws Exception
    {
        ProjectSVNStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
        PathElement path = PathElement.createFile("UnitTest.as", null, null);
        SwiftlyDocument doc = storage.getDocument(path);

        // Ensure the document data was defrosted correctly
        assertTrue(doc.getText().startsWith("package {"));
    }

    public void testPutDocument ()
        throws Exception
    {
        ProjectSVNStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
        PathElement path = PathElement.createFile("UnitTest.as", null, null);
        SwiftlyDocument doc;
        
        // Get an initial copy of the document
        doc = storage.getDocument(path);

        // Modify and commit the changes
        doc.setText("Modified");
        storage.putDocument(doc, "Testing");

        // Retrieve the document again
        doc = storage.getDocument(path);
        assertEquals("Modified", doc.getText());
    }

    public void testGetVNURL ()
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

    /** Try exporting a project to disk. */
    public void testExportProject ()
        throws Exception
    {
        ProjectStorage storage = new ProjectSVNStorage(_projectRecord, _storageRecord);
        File exportDir = new File(_tempDir, "export");
        File srcFile = new File(exportDir, "UnitTest.as");

        storage.export(exportDir);
        assertTrue(srcFile.getName() + " was not exported.", srcFile.exists());

        // Ensure the document data was exported correctly.
        InputStream input = new FileInputStream(srcFile);
        byte data[] = new byte[1024];
        int len;
        String contents;
    
        assertTrue("Could not read any data from " + srcFile.getName(),
            (len = input.read(data, 0, data.length)) >= 0);

        contents = new String(data, 0, len, ProjectSVNStorage.TEXT_ENCODING);
        assertTrue("Unexpected file data: " + contents, contents.startsWith("package {"));
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
