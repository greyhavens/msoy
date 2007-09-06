//
// $Id$

package com.threerings.msoy.swiftly.data;

import junit.framework.TestCase;

public class FlexCompilerOutputUnitTest extends TestCase
{
    public static final String FAKE_BUILD_ROOT =
        "/export/msoy/data/swiftly/buil/localbuilder1234_1";

    public FlexCompilerOutputUnitTest (String name)
    {
        super(name);
    }

    public void testParsing ()
    {
        CompilerOutput output = new FlexCompilerOutput(
            FAKE_BUILD_ROOT + "/Mirror/Mirror.as(39): col: 50 Error: Something bad happened");
        assertEquals(39, output.getLineNumber());
        assertEquals(50, output.getColumnNumber());
        assertEquals("Mirror.as", output.getFileName());
        assertEquals("/Mirror/Mirror.as", output.getPath());
        assertEquals(CompilerOutput.Level.ERROR, output.getLevel());
        assertEquals("Something bad happened", output.getMessage());
    }

    public void testUnparsableMessage ()
    {
        CompilerOutput output = new FlexCompilerOutput("Hello, World");
        assertEquals(-1, output.getLineNumber());
        assertEquals(-1, output.getColumnNumber());
        assertEquals(null, output.getFileName());
        assertEquals(CompilerOutput.Level.UNKNOWN, output.getLevel());
        assertEquals("Hello, World", output.getMessage());
    }
}
