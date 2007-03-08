//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import junit.framework.TestCase;

public class FlexCompilerOutputUnitTest extends TestCase
{
    public FlexCompilerOutputUnitTest (String name)
    {
        super(name);
    }

    public void testParsing ()
    {
        CompilerOutput output = new FlexCompilerOutput("/srcpath/Mirror/Mirror.as(39): col: 50 " +
            "Error: Something bad happened");
        assertEquals(39, output.getLineNumber());
        assertEquals(50, output.getColumnNumber());
        assertEquals("/srcpath/Mirror/Mirror.as", output.getFileName());
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
