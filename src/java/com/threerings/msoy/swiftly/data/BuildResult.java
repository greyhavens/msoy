//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.List;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.server.build.BuildArtifact;
import com.threerings.presents.dobj.DSet;

/**
 * DObject version of the build results.
 */
public class BuildResult
    implements Streamable, DSet.Entry
{
    // from Streaming
    public BuildResult ()
    {
    }

    /** Construct a BuildResult using a BuildArtifact */
    public BuildResult (BuildArtifact artifact, MemberName member)
    {
        _output = artifact.getOutput();
        _buildSuccess = artifact.buildSuccessful();
        _buildTime = artifact.getBuildTime();
        _member = member;
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return _member;
    }

    public long getBuildTime ()
    {
        return _buildTime;
    }

    public MemberName getMember()
    {
        return _member;
    }

    /** Return the build compiler's output, in the order it was received. */
    public List<CompilerOutput> getOutput ()
    {
        return _output;
    }

    /** Returns true if the build succeeded. */
    public boolean buildSuccessful ()
    {
        return _buildSuccess;
    }

    /** The member who this result is for. */
    protected MemberName _member;

    /** All compiler output. */
    protected List<CompilerOutput> _output;

    /** Did the build succeed. */
    protected boolean _buildSuccess;

    /** The time, in milliseconds, that the full build task took. */
    protected long _buildTime;
}
