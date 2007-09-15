//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.web.data.SwiftlyProject;

public class ProjectSVNStorageUnitTest extends TestCase
{
    /** Mock up an SVN storage record. */
    public static SwiftlySVNStorageRecord mockStorageRecord (final File tempDir)
    {
        SwiftlySVNStorageRecord storageRecord;

        storageRecord = new SwiftlySVNStorageRecord();
        storageRecord.protocol = ProjectSVNStorage.PROTOCOL_FILE;
        storageRecord.baseDir = tempDir.getAbsolutePath();

        return storageRecord;
    }

    public ProjectSVNStorageUnitTest (final String name)
    {
        super(name);
    }

    /** Set up the test case anew. */
    @Override
    public void setUp ()
        throws Exception
    {
        // Create a temporary directory.
        _tempDir = File.createTempFile("svnstorage", "test");
        _tempDir.delete();
        if (_tempDir.mkdir() != true) {
            throw new Exception("Temporary directory '" + _tempDir + "' already exists!");
        }

        _project = ProjectStorageUnitTest.mockProject();
        _storageRecord = mockStorageRecord(_tempDir);

        // Initialize the storage
        ProjectSVNStorage.initializeStorage(_project, _storageRecord,
            ProjectStorageUnitTest.TEMPLATE_DIR.getCanonicalFile());
    }


    /** Clean up afterwards. */
    @Override
    public void tearDown ()
        throws Exception
    {
        FileUtils.deleteDirectory(_tempDir);
    }

    /** Try opening of a project. */
    public void testOpenStorage ()
        throws Exception
    {
        final ProjectStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        assertTrue(storage != null);
    }


    public void testGetDocument ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final PathElement path = PathElement.createFile("UnitTest.as", null, "text/x-actionscript");
        final SwiftlyTextDocument doc = (SwiftlyTextDocument)storage.getDocument(path);

        // Ensure the document data was defrosted correctly
        assertTrue(doc.getText().startsWith("package {"));
    }

    public void testPutDocument ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final PathElement path = PathElement.createFile("UnitTest.as", null, "text/x-actionscript");
        SwiftlyTextDocument doc;

        // Get an initial copy of the document
        doc = (SwiftlyTextDocument)storage.getDocument(path);

        // Modify and commit the changes
        doc.setText("Modified");
        storage.putDocument(doc, "Testing");

        // Retrieve the document again
        doc = (SwiftlyTextDocument)storage.getDocument(path);
        assertEquals("Modified", doc.getText());
    }

    /** If a non-svnkit exception was thrown in the middle of a commit operation,
     *  the transaction would not be safely aborted. Test that the transaction is
     *  aborted safely and the svnkit repository instance is left in a consistent
     *  state. */
    public void testPutDocumentReentrancy ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final PathElement path = PathElement.createFile("UnitTest.as", null, "text/x-actionscript");

        // Create and initialize broken document
        SwiftlyDocument brokenDoc = new SwiftlyIOExceptionDocument(
            new ByteArrayInputStream(new byte[0]), path);

        // Commit the IOException-raising document
        try {
            storage.putDocument(brokenDoc, "Testing");
            fail("Did not throw an IOException");
        } catch (final ProjectStorageException e) {
            // Supposed to fail
        }

        // Attempt to refetch the document. If the commit was not aborted safely,
        // svnkit will throw a re-entrancy exception.
        brokenDoc = storage.getDocument(path);
    }

    public void testDeleteDocument ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final PathElement path = PathElement.createFile("UnitTest.as", null, "text/x-actionscript");
        SwiftlyTextDocument doc;

        // Get an initial copy of the document
        doc = (SwiftlyTextDocument)storage.getDocument(path);

        // Modify and commit the changes
        doc.setText("Modified");
        storage.putDocument(doc, "Testing");

        // Delete the document
        storage.deleteDocument(path, "Deleting");

        // Retrieve the document again
        try {
            doc = (SwiftlyTextDocument)storage.getDocument(path);
            fail("The storage engine did not delete the document!");
        } catch (final ProjectStorageException pse) {
            // It's supposed to fail!
        }
    }

    public void testRenameDocument ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final PathElement path = PathElement.createFile("UnitTest.as", null, "text/x-actionscript");
        SwiftlyTextDocument doc;

        // Get an initial copy of the document
        doc = (SwiftlyTextDocument)storage.getDocument(path);

        // Modify and commit the changes
        doc.setText("Modified");
        storage.putDocument(doc, "Testing");

        // Delete the document
        storage.renameDocument(path, "UnitTestMoved.as", "Renaming");

        // Retrieve the new document
        final PathElement newPath = PathElement.createFile(
            "UnitTestMoved.as", null, "text/x-actionscript");
        try {
            doc = (SwiftlyTextDocument)storage.getDocument(newPath);
        } catch (final ProjectStorageException pse) {
            fail("The storage engine did not rename the document!");
        }

        // Retrieve the old document
        try {
            doc = (SwiftlyTextDocument)storage.getDocument(path);
            fail("The storage engine did not delete the renamed document!");
        } catch (final ProjectStorageException pse) {
            // It's supposed to fail!
        }
    }

    public void testGetSVNURL ()
        throws Exception
    {
        final ProjectSVNStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        assertEquals("file://" + _tempDir + "/" + _project.projectId,
            storage.getSVNURL().toString());
    }

    /** Try listing a project. */
    public void testGetProjectTree ()
        throws Exception
    {
        final ProjectStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final List<PathElement> projectTree = storage.getProjectTree();
        assertTrue("The returned PathElement list is empty", projectTree.size() != 0);

        // Simple sanity check of one of the paths, to ensure that it matches the template
        boolean foundPNGFile = false;
        boolean foundASFile = false;

        for (final PathElement node : projectTree) {
            if (node.getName().equals("test.png")) {
                foundPNGFile = true;
                final PathElement parent = node.getParent();
                assertEquals("media", parent.getName());
                assertEquals(PathElement.Type.ROOT, parent.getParent().getType());
                assertEquals("image/png", node.getMimeType());
            }
            if (node.getName().equals("UnitTest.as")) {
                foundASFile = true;
                final PathElement parent = node.getParent();
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
        final ProjectStorage storage = new ProjectSVNStorage(_project, _storageRecord);
        final File exportDir = new File(_tempDir, "export");
        final File srcFile = new File(exportDir, "UnitTest.as");

        storage.export(exportDir);
        assertTrue(srcFile.getName() + " was not exported.", srcFile.exists());

        // Ensure the document data was exported correctly.
        final InputStream input = new FileInputStream(srcFile);
        final byte data[] = new byte[1024];
        int len;
        String contents;

        assertTrue("Could not read any data from " + srcFile.getName(),
            (len = input.read(data, 0, data.length)) >= 0);

        contents = new String(data, 0, len, ProjectStorage.TEXT_ENCODING);
        assertTrue("Unexpected file data: " + contents, contents.startsWith("package {"));
    }


    /**
     * Always throws an IOException when getModifiedData() is called.
     * Used to test for commit operation re-entrancy.
     */
    protected static class SwiftlyIOExceptionDocument
        extends SwiftlyDocument
    {
        public SwiftlyIOExceptionDocument (final ByteArrayInputStream stream, final PathElement path)
            throws IOException
        {
            super(stream, path);
        }

        @Override
        public InputStream getModifiedData ()
            throws IOException
        {
            final InputStream dead = new FileInputStream("/file does not ever exist");
            return dead;
        }

        public boolean handlesMimeType (final String mimeType) {
            return true;
        }

        @Override
        public void loadInEditor (final SwiftlyDocumentEditor editor, PositionLocation location)
        {
            return;
        }

        @Override
        public boolean isDirty () {
            return true;
        }

        @Override
        public void setData (final InputStream input, final String encoding) {
            return;
        }
    }

    /** Temporary test directory. */
    protected File _tempDir;

    /** Mocked up project record. */
    protected SwiftlyProject _project;

    /** Mocked up storage record. */
    protected SwiftlySVNStorageRecord _storageRecord;
}
