//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.ByteArrayInputStream;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;

import junit.framework.TestCase;

public class SwiftlyDocumentUnitTest extends TestCase
{
    public SwiftlyDocumentUnitTest (String name)
    {
        super(name);
    }

    public void testCreateFromPathElement ()
        throws Exception
    {
        // create the new PathElement
        PathElement element = PathElement.createFile("testfile", null, "text/plain");

        // create the new SwiftlyDocument
        ByteArrayInputStream data = new ByteArrayInputStream(new byte[0]);
        SwiftlyDocument doc = SwiftlyDocument.createFromPathElement(data, element,
            ProjectStorage.TEXT_ENCODING);
        assertNotNull(doc);
    }
}
