//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.dobj.DSet;

/**
 * Compilation Build Results.
 */
public class BuildResult
    implements Streamable, DSet.Entry
{
    // from Streaming
    public BuildResult ()
    {
    }

    public BuildResult (MemberName member)
    {
        _member = member;
        _output = new ArrayList<CompilerOutput>();
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return _member;
    }

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

    public MemberName getMember()
    {
        return _member;
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

    /** The member who this result is for. */
    protected MemberName _member;

    /** All compiler output. */
    protected List<CompilerOutput> _output;

    /** Did the build succeed. */
    protected boolean _buildSuccess = true;

    /** The time, in milliseconds, that the full build task took. */
    protected long _buildTime;

    /** The build output file. */
    protected transient File _outputFile;
}
