//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import junit.framework.TestCase;

public class BuildResultUnitTest extends TestCase
{
    public BuildResultUnitTest (String name)
    {
        super(name);
    }

    public void testAppendOutput ()
    {
        CompilerOutput output = new FlexCompilerOutput("Awesome",
            CompilerOutput.Level.INFO, "file.as", 27);
        BuildResult result = new BuildResult();

        result.appendOutput(output);
        
        for (CompilerOutput testOutput : result.getOutput()) {
            assertEquals(testOutput, output);
        }
    }
}
