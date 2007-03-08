//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.util.LinkedList;
import java.util.List;

/**
 * Compilation Build Results.
 */
public class BuildResult
{

    public BuildResult ()
    {
        _output = new LinkedList<CompilerOutput>();
    }

    /** Return the build compiler's output, in the order it was received. */
    public List<CompilerOutput> getOutput () {
        return _output;
    }

    /** Append a parsed compiler statement to the build output. */
    public void appendOutput (CompilerOutput output) {
        _output.add(output);
    }

    /** All compiler output. */
    private List<CompilerOutput> _output;
}
