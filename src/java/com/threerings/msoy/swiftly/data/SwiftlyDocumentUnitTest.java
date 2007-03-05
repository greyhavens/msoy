//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

public class SwiftlyDocumentUnitTest extends TestCase
{
    public SwiftlyDocumentUnitTest (String name)
    {
        super(name);
    }

    public void testInstantiate ()
        throws Exception
    {
        File inputFile = File.createTempFile("source", ".swiftly-storage");
        inputFile.deleteOnExit();

        OutputStream output = new FileOutputStream(inputFile);
        InputStream input = new FileInputStream(inputFile);
        output.write("Hello, World".getBytes(TEXT_ENCODING));
        
        SwiftlyDocument doc = new SwiftlyDocument(input, null, TEXT_ENCODING);
        assertEquals("Hello, World", doc.getText());
    }

    protected static final String TEXT_ENCODING = "utf8";
}
