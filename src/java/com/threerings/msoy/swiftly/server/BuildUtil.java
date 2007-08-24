//
// $Id$
package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.samskivert.io.PersistenceException;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.swiftly.server.build.BuildArtifact;
import com.threerings.msoy.swiftly.server.build.ProjectBuilder;
import com.threerings.msoy.swiftly.server.build.ProjectBuilderException;

/**
 * Utility class for building an AbstractBuildTask.
 */
public class BuildUtil
{
    /**
     * Build the project and return the BuildArtifact. Any file system results will be deleted.
     * @throws IOException, ProjectBuilderException, PersistenceException
     */
    public static BuildArtifact buildProject (AbstractBuildTask task, ProjectBuilder builder)
        throws IOException, ProjectBuilderException, PersistenceException
    {
        final BuildArtifact artifact;
        File buildDir = null;
        final long startTime = System.currentTimeMillis();
        try {
            // Get the local build directory
            File topBuildDir =
                new File(ServerConfig.serverRoot + ProjectRoomManager.LOCAL_BUILD_DIRECTORY);

            // Create a temporary build directory
            buildDir = File.createTempFile(
                "localbuilder", "_" + String.valueOf(task.getProjectId()), topBuildDir);
            buildDir.delete();
            if (buildDir.mkdirs() != true) {
                // This should -never- happen, try to exit gracefully.
                log.warning("Unable to create swiftly build directory: " + buildDir);
                throw new IOException(
                    "Unable to create swiftly build directory. [dir=" + buildDir + "].");
            }

            // build the project
            artifact = builder.build(buildDir);

            // let the build task do any result processing while the build artifact still exists,
            // if a build artifact was created
            if (artifact.getOutputFile().exists()) {
                task.processArtifact(artifact);
            }

            // set the full time of the build in the result
            artifact.setBuildTime(System.currentTimeMillis() - startTime);

            return artifact;

        } finally {
            // finally clean up the build results.
            try {
                if (buildDir != null) {
                    FileUtils.deleteDirectory(buildDir);
                }
            } catch (IOException ioe) {
                // only log to the server if this fails, client doesn't care.
                log.log(Level.WARNING,
                    "Failed to delete temporary build results directory.", ioe);
            }
        }
    }
}
