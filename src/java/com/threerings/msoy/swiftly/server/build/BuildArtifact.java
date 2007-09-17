//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.threerings.msoy.swiftly.data.CompilerOutput;

/**
 * Server side class to store the build results.
 */
public class BuildArtifact
{
    /** Set compiler output file path. */
    public void setOutputFile (File path)
    {
        _outputFile = path;
    }

    public File getOutputFile ()
    {
        return _outputFile;
    }

    /** Set the time, in milliseconds, that the full build task took. */
    public void setBuildTime (long time)
    {
        _buildTime = time;
    }

    public long getBuildTime ()
    {
        return _buildTime;
    }

    /** Return the build compiler's output, in the order it was received. */
    public List<CompilerOutput> getOutput ()
    {
        return _output;
    }

    /** Append a parsed compiler statement to the build output. */
    public void appendOutput (CompilerOutput output)
    {
        // If we get an error message, the build failed.
        if (output.getLevel() == CompilerOutput.Level.ERROR) {
            _buildSuccess = false;
        }

        _output.add(output);
    }

    /** Returns true if the build succeeded. */
    public boolean buildSuccessful ()
    {
        return _buildSuccess;
    }

    /** All compiler output. */
    private final List<CompilerOutput> _output = new ArrayList<CompilerOutput>();

    /** Did the build succeed. */
    private boolean _buildSuccess = true;

    /** The time, in milliseconds, that the full build task took. */
    private long _buildTime;

    /** The build output file. */
    private File _outputFile;
}
