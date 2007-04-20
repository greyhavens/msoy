//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

public class SwiftlyTextDocumentUnitTest extends TestCase
{
    public SwiftlyTextDocumentUnitTest (String name)
    {
        super(name);
    }

    public void setUp ()
        throws Exception
    {
        File inputFile = File.createTempFile("source", ".swiftly-storage");
        inputFile.deleteOnExit();

        OutputStream output = new FileOutputStream(inputFile);
        output.write("Hello, World".getBytes(TEXT_ENCODING));

        InputStream input = new FileInputStream(inputFile);
    
        _doc = new SwiftlyTextDocument(input, null, TEXT_ENCODING);
    }

    public void testInstantiate ()
        throws Exception
    {        
        assertEquals("Hello, World", _doc.getText());
    }

    public void testGetOriginalData ()
        throws Exception
    {
        byte data[] = new byte[1024];
        int len;

        /* Read from the original data. */
        assertTrue((len = _doc.getOriginalData().read(data, 0, data.length)) > 0);

        String text = new String(data, 0, len, _doc.getTextEncoding());
        assertEquals("Hello, World", text);
    }

    public void testGetModifiedData ()
        throws Exception
    {
        byte data[] = new byte[1024];
        int len;

        /* Read from the original data. */
        assertTrue((len = _doc.getModifiedData().read(data, 0, data.length)) > 0);

        String text = new String(data, 0, len, _doc.getTextEncoding());
        assertEquals("Hello, World", text);
    }

    public void testIsDirty ()
        throws Exception
    {
        assertTrue(!_doc.isDirty());

        // document hasn't actually changed from backing store so should be false
        _doc.setText("Hello, World");
        assertTrue(!_doc.isDirty());

        // document has changed from backing store so should be true
        _doc.setText("Goodbye, World");
        assertTrue(_doc.isDirty());
    }

    protected static final String TEXT_ENCODING = "utf8";
    
    protected SwiftlyTextDocument _doc;
}
