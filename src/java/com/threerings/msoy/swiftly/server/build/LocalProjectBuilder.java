//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.FlexCompilerOutput;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Server-local project builder.
 * TODO: Cache and update checkouts.
 */
public class LocalProjectBuilder
    implements ProjectBuilder
{
    /**
     * Create a new local project builder.
     * @param flexSDK: Local path to the Flex SDK.
     */
    public LocalProjectBuilder (SwiftlyProject project, ProjectStorage storage,
                                File flexSDK, File whirledSDK, File serverRoot)
    {
        _project = project;
        _storage = storage;
        _flexSDK = flexSDK;
        _whirledSDK = whirledSDK;
        _serverRoot = serverRoot;
    }

    public BuildArtifact build (File buildRoot)
        throws ProjectBuilderException
    {
        // Export the project data
        try {
            _storage.export(buildRoot);
        } catch (ProjectStorageException pse) {
            throw new ProjectBuilderException("Exporting project data from storage failed: " + pse,
                pse);
        }

        // Build the project
        // TODO: The mxmlc process and #include any file on the system.
        // We need to start it under a permissions-limited JVM.
        try {
            Process proc;
            ProcessBuilder procBuilder;
            InputStream stdout;
            BufferedReader bufferedOutput;
            LinkedList<String> outputQueue;
            BuildArtifact artifact = new BuildArtifact();
            String line;

            // Refer the to "Using the Flex Compilers" documentation
            // http://livedocs.adobe.com/flex/2/docs/00001477.html
            procBuilder = new ProcessBuilder(
                _serverRoot + "/bin/swiftlycompiler",
                "--kill_delay=" + KILL_DELAY_DEFAULT,
                "-load-config",
                _whirledSDK.getAbsolutePath() + "/etc/whirled-config.xml",
                "-compiler.source-path=" + buildRoot.getAbsolutePath(),
                "+flexlib=" + _flexSDK.getAbsolutePath() + "/frameworks",
                "+flex_sdk=" + _flexSDK.getAbsolutePath(),
                "+whirled_sdk=" + _whirledSDK.getAbsolutePath(),
                "-file-specs",
                _project.getTemplateSourceName()
            );

            // Set the working directory to the build root.
            procBuilder.directory(buildRoot);

            // Direct stderr to stdout.
            procBuilder.redirectErrorStream(true);

            // Run the process and gather output
            proc = procBuilder.start();

            stdout = proc.getInputStream();
            bufferedOutput = new BufferedReader(new InputStreamReader(stdout));
            outputQueue = new LinkedList<String>();

            while ((line = bufferedOutput.readLine()) != null) {
                // store the raw compiler output in a queue used for logging/debugging
                outputQueue.add(line);
                CompilerOutput output = new FlexCompilerOutput(line);
                switch (output.getLevel()) {
                case IGNORE:
                    continue;
                case UNKNOWN:
                    // log.warning("Unparsable swiftly flex compiler output. [line=" + line + "]");
                    break;
                }
                artifact.appendOutput(output);
                // trim the output queue so that massive amounts of output do not fill up memory
                if (outputQueue.size() > MAX_QUEUE_SIZE) {
                    outputQueue.removeFirst();
                }
            }

            // block this thread until the compiler thread finishes
            int exitCode = proc.waitFor();

            // if we had a successful build yet had a non 0 exit code, something wacky happened
            if (artifact.buildSuccessful() && exitCode > 0) {
                throw new ProjectBuilderException("Successful build returned non-zero exit " +
                    "value. [code=" + exitCode + ",output=" + outputQueue + "].");
            }

            // if we had a successful build yet did not generate a build result, throw exception
            File outputFile = new File(buildRoot, _project.getOutputFileName());
            if (artifact.buildSuccessful() && !outputFile.exists()) {
                throw new ProjectBuilderException("Successful build did not produce a build " +
                    "result. [output=" + outputQueue + "].");
            }

            artifact.setOutputFile(outputFile);

            return artifact;

        } catch (IOException ioe) {
            throw new ProjectBuilderException.InternalError(
                "Failed to execute build process: " + ioe, ioe);

        } catch (InterruptedException ie) {
            throw new ProjectBuilderException.InternalError(
                "Failed to finish build process. Process interrupted. " + ie, ie);
        }
    }

    /** The maximum number of lines to store in the local process output queue */
    protected static final int MAX_QUEUE_SIZE = 40;

    /** Number of milliseconds after which the compile task will kill itself. */
    public static final long KILL_DELAY_DEFAULT = 180000;

    /** Reference to our project. */
    protected final SwiftlyProject _project;

    /** Reference to our backing project storage. */
    protected final ProjectStorage _storage;

    /** Path to the Flex SDK. */
    protected final File _flexSDK;

    /** Path to the Whirled SDK. */
    protected final File _whirledSDK;

    /** Path to the base of the msoy server */
    protected final File _serverRoot;
}
