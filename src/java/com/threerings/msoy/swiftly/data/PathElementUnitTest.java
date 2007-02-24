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
        PathElement parent = PathElement.createDirectory("Directory", null);
        PathElement child = PathElement.createFile("File", parent);
        
        assertEquals("/Directory/File", child.getAbsolutePath());
    }

    public void testEquals ()
    {
        PathElement parent1 = PathElement.createDirectory("Directory", null);
        PathElement parent2 = PathElement.createDirectory("Directory", null);
        
        PathElement child1 = PathElement.createFile("File", parent1);
        PathElement child2 = PathElement.createFile("File", parent2);
        
        assertEquals(child1, child2);   
    }

}
