//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.threerings.msoy.swiftly.data.BuildResult;
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
        File flexSDK, File whirledSDK)
    {
        _project = project;
        _storage = storage;
        _flexSDK = flexSDK;
        _whirledSDK = whirledSDK;
    }

    public BuildResult build (File buildRoot)
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
            StringBuffer rawOutput;
            BuildResult result = new BuildResult();
            String line;

            // Refer the to "Using the Flex Compilers" documentation
            // http://livedocs.adobe.com/flex/2/docs/00001477.html
            procBuilder = new ProcessBuilder(
                _flexSDK.getAbsolutePath() + "/bin/mxmlc",
                "-load-config",
                _whirledSDK.getAbsolutePath() + "/etc/whirled-config.xml",
                "-compiler.source-path=.",
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
            rawOutput = new StringBuffer();

            while ((line = bufferedOutput.readLine()) != null) {
                rawOutput.append(line);
                CompilerOutput output = new FlexCompilerOutput(line);
                switch (output.getLevel()) {
                case IGNORE:
                    continue;
                case UNKNOWN:
                    // log.warning("Unparsable swiftly flex compiler output. [line=" + line + "]");
                    break;
                }
                result.appendOutput(output);
            }

            // if we had a successful build yet did not generate a build result, throw exception
            File outputFile = new File(buildRoot, _project.getOutputFileName());
            if (result.buildSuccessful() && !outputFile.exists()) {
                throw new ProjectBuilderException("Successful build did not produce a build " +
                    "result. [output=" + rawOutput + "].");
            }

            result.setOutputFile(outputFile);

            return result;

        } catch (IOException ioe) {
            throw new ProjectBuilderException.InternalError(
                "Failed to execute build process: " + ioe, ioe);
        }
    }

    /** Reference to our project. */
    protected SwiftlyProject _project;

    /** Reference to our backing project storage. */
    protected ProjectStorage _storage;

    /** Path to the Flex SDK. */
    protected File _flexSDK;

    /** Path to the Whirled SDK. */
    protected File _whirledSDK;
}
