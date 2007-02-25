//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

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
        SVNURL svnURL;
        
        // Create a temporary directory.
        _tempDir = File.createTempFile("svnstorage", "test");
        _tempDir.delete();
        if (_tempDir.mkdir() != true) {
            throw new Exception("Temporary directory '" + _tempDir + "' already exists!");
        }

        // Create a subversion URL and test repository.
        svnDir = new File(_tempDir, "svn-repo");
        svnURL = SVNURL.fromFile(svnDir);
        SVNRepositoryFactory.createLocalRepository(svnDir, true, false);

        // Mock up a project record.
        _projectRecord = new SwiftlyProjectRecord();
        _projectRecord.projectSubversionURL = svnURL.toString();

        // Initialize the storage
        ProjectSVNStorage.initializeStorage(_projectRecord);
    }

    /** Recursively delete a directory. */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }

    /** Clean up afterwards. */
    public void tearDown ()
    {
        deleteDir(_tempDir);
    }



    /** Try opening of a project. */
    public void testOpenProject ()
        throws Exception
    {
        ProjectStorage project = new ProjectSVNStorage(_projectRecord);
    }


    /** Try listing a project. */
    public void testGetProjectTree ()
        throws Exception
    {
        ProjectStorage project = new ProjectSVNStorage(_projectRecord);
        List projectTree = project.getProjectTree();
        assertTrue("The returned PathElement list is empty", projectTree.size() != 0);
    }

    /** Temporary test directory. */
    protected File _tempDir;

    /** Mocked up project record. */
    protected SwiftlyProjectRecord _projectRecord;
}
