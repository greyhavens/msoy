//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.ArrayList;
import java.util.List;

import com.threerings.io.Streamable;
import com.threerings.msoy.swiftly.server.build.BuildArtifact;

/**
 * DObject version of the build results.
 */
public class BuildResult
    implements Streamable
{
    // from Streaming
    public BuildResult ()
    {
    }

    /** Construct a BuildResult using a BuildArtifact */
    public BuildResult (BuildArtifact artifact)
    {
        _output = artifact.getOutput();
        _buildSuccess = artifact.buildSuccessful();
        _buildTime = artifact.getBuildTime();
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

    /** Return the list of CompilerOutput lines associated with the supplied PathElement. */
    public List<CompilerOutput> getOutputForPath (PathElement element)
    {
        List<CompilerOutput> lines = new ArrayList<CompilerOutput>();
        for (CompilerOutput line : _output) {
            if (line.getPath() != null) {
                if (element.getAbsolutePath().equals(line.getPath())) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /** Returns true if the build succeeded. */
    public boolean buildSuccessful ()
    {
        return _buildSuccess;
    }

    /** All compiler output. */
    private List<CompilerOutput> _output;

    /** Did the build succeed. */
    private boolean _buildSuccess;

    /** The time, in milliseconds, that the full build task took. */
    private long _buildTime;
}
