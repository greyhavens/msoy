//
// $Id$

package com.threerings.msoy.swiftly.data;

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
            CompilerOutput.Level.INFO, "file.as", 27, 5);
        BuildResult result = new BuildResult();

        result.appendOutput(output);
        
        for (CompilerOutput testOutput : result.getOutput()) {
            assertEquals(testOutput, output);
        }
    }
}
