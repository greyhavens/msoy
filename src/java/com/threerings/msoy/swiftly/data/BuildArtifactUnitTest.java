//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;

import junit.framework.TestCase;

import com.threerings.msoy.swiftly.server.build.BuildArtifact;

public class BuildArtifactUnitTest extends TestCase
{
    public BuildArtifactUnitTest (String name)
    {
        super(name);
    }

    public void testAppendOutput ()
    {
        CompilerOutput output = new FlexCompilerOutput("Awesome",
            CompilerOutput.Level.INFO, "file.as", 27, 5);
        BuildArtifact result = new BuildArtifact();

        result.appendOutput(output);

        for (CompilerOutput testOutput : result.getOutput()) {
            assertEquals(testOutput, output);
        }
    }

    public void testErrorDetection ()
    {
        CompilerOutput output = new FlexCompilerOutput("Awesome",
            CompilerOutput.Level.ERROR, "file.as", 27, 5);
        BuildArtifact result = new BuildArtifact();

        assertEquals(true, result.buildSuccessful());
        result.appendOutput(output);
        assertEquals(false, result.buildSuccessful());
    }

    public void testSetBuildOutputFile ()
    {
        BuildArtifact result = new BuildArtifact();
        File outputFile = new File("/tmp/nonexistent");

        result.setOutputFile(outputFile);
    }
}
