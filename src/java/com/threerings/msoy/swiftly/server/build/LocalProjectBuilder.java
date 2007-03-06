//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * Server-local project builder.
 * TODO: Cache and update checkouts.
 */
public class LocalProjectBuilder
    implements ProjectBuilder
{
    /**
     * Create a new local project builder.
     * @param flexPath: Local path to the Flex SDK.
     */
    public LocalProjectBuilder (SwiftlyProjectRecord record, ProjectStorage storage,
        File buildRoot, File flexPath)
        throws ProjectBuilderException
    {
        _projectRecord = record;
        _storage = storage;
        _flexPath = flexPath;
        
        // Create a temporary build directory
        try {
            _buildRoot = File.createTempFile(record.projectName, "build", buildRoot);
            _buildRoot.delete();
            if (_buildRoot.mkdir() != true) {
                throw new ProjectBuilderException("Unable to create temporary build root " +
                    "directory, directory already exists: " + _buildRoot);
            }
        } catch (IOException ioe) {
            throw new ProjectBuilderException("Unable to create temporary build root '" +
                _buildRoot + "': " + ioe, ioe);            
        }
    }

    public void build ()
        throws ProjectBuilderException
    {
        // Export the project data
        try {
            _storage.export(_buildRoot);            
        } catch (ProjectStorageException pse) {
            throw new ProjectBuilderException("Exporting project data from storage failed: " + pse,
                pse);
        }

        try {
            // XXX Totally fragile, broken, and otherwise insecure!
            String[] cmd = {
                _flexPath.getAbsolutePath() + "/bin/mxmlc",
                "-load-config",
                "data/temp_sdk/templates/whirled-config.xml",
                "-compiler.source-path=" + _buildRoot.getAbsolutePath(),
                "-file-specs",
                "Game.as"
            };
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            // Do nothing, this is broken after all.
        }
    }

    /** Be sure to delete our build root. */
    protected void finalize ()
        throws Throwable
    {
        try {
            FileUtils.deleteDirectory(_buildRoot);
        } finally {
            super.finalize();
        }
    }
    /** Reference to our project record. */
    protected SwiftlyProjectRecord _projectRecord;

    /** Reference to our backing project storage. */
    protected ProjectStorage _storage;
    
    /** Path to the Flex SDK. */
    protected File _flexPath;

    /** Instance-specific build directory. */
    protected File _buildRoot;
}
