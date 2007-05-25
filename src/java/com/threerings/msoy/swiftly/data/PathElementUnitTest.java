//
// $Id$

package com.threerings.msoy.swiftly.data;

import junit.framework.TestCase;

public class PathElementUnitTest extends TestCase
{
    public PathElementUnitTest (String name)
    {
        super(name);
    }

    public void testGetAbsolutePath ()
    {
        PathElement root = PathElement.createRoot("Project Name");
        assertEquals("", root.getAbsolutePath());
        
        PathElement parent = PathElement.createDirectory("Directory", root);
        PathElement child = PathElement.createFile("File", parent, "text/plain");
        
        assertEquals("/Directory/File", child.getAbsolutePath());
    }

    public void testEquals ()
    {
        PathElement parent1 = PathElement.createDirectory("Directory", null);
        PathElement parent2 = PathElement.createDirectory("Directory", null);
        
        PathElement child1 = PathElement.createFile("File", parent1, "text/plain");
        PathElement child2 = PathElement.createFile("File", parent2, "text/plain");
        
        assertEquals(child1, child2);   
    }

}
