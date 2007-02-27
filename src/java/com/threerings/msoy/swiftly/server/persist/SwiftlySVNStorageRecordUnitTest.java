//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;

import junit.framework.TestCase;

public class SwiftlySVNStorageRecordUnitTest extends TestCase
{
    public SwiftlySVNStorageRecordUnitTest (String name)
    {
        super(name);
    }

    /**
     * Test file:// URI assembly
     */
    public void testToFileURI ()
        throws Exception
    {
        // Mock up a file:// record
        SwiftlySVNStorageRecord record = new SwiftlySVNStorageRecord();
        record.protocol = ProjectSVNStorage.PROTOCOL_FILE;
        record.baseDir = "/var/lib/ectoplasm/ghostbusters";
        assertEquals("file://" + record.baseDir, record.toURI().toString());
    }

    /**
     * Test out non-file:// URI assembly
     */
    public void testToSVNURI ()
        throws Exception
    {
        // Mock up a svn:// record
        SwiftlySVNStorageRecord record = new SwiftlySVNStorageRecord();
        record.protocol = ProjectSVNStorage.PROTOCOL_SVN;
        record.host = "ghostbusters.com";
        record.baseDir = "/repos/ectoplasm/ghostbusters";
        assertEquals("svn://" + record.host + record.baseDir, record.toURI().toString());
    }

    /**
     * Test URI assembly with an included port;
     */
    public void testToURIWithPort ()
        throws Exception
    {
        // Mock up a svn:// record
        SwiftlySVNStorageRecord record = new SwiftlySVNStorageRecord();
        record.protocol = ProjectSVNStorage.PROTOCOL_HTTPS;
        record.host = "ghostbusters.com";
        record.port = 555;
        record.baseDir = "/repos/ectoplasm/ghostbusters";
        assertEquals("https://" + record.host + ":" + record.port + record.baseDir, record.toURI().toString());
    }
}
