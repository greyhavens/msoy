//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.sql.Timestamp;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.io.PersistenceException;

import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;

import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Contains the definition of a swiftly svn-based project storage.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"svnProtocol", "host", "port", "baseDir"})})
public class SwiftlySVNStorageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #storageId} field. */
    public static final String STORAGE_ID = "storageId";

    /** The qualified column identifier for the {@link #storageId} field. */
    public static final ColumnExp STORAGE_ID_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, STORAGE_ID);

    /** The column identifier for the {@link #svnProtocol} field. */
    public static final String SVN_PROTOCOL = "svnProtocol";

    /** The qualified column identifier for the {@link #svnProtocol} field. */
    public static final ColumnExp SVN_PROTOCOL_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, SVN_PROTOCOL);

    /** The column identifier for the {@link #host} field. */
    public static final String HOST = "host";

    /** The qualified column identifier for the {@link #host} field. */
    public static final ColumnExp HOST_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, HOST);

    /** The column identifier for the {@link #port} field. */
    public static final String PORT = "port";

    /** The qualified column identifier for the {@link #port} field. */
    public static final ColumnExp PORT_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, PORT);

    /** The column identifier for the {@link #baseDir} field. */
    public static final String BASE_DIR = "baseDir";

    /** The qualified column identifier for the {@link #baseDir} field. */
    public static final ColumnExp BASE_DIR_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, BASE_DIR);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The id of the project. */
    @Id
    @GeneratedValue
    public int storageId;

    /** The SVN protocol (svn+ssh, svn, https, etc). */
    public String svnProtocol;

    /** The storage host (FQDN). */
    @Column(nullable=true)
    public String host;

    /** The storage port number. */
    @Column(nullable=true)
    public int port;

    /** The storage base directory. */
    public String baseDir;
    
    public URI toURI ()
        throws URISyntaxException
    {        
        if (svnProtocol == ProjectSVNStorage.PROTOCOL_FILE) {
            // Confusingly (buggily?) one must set an empty authority in order for URI
            // to properly append '//' to the file: scheme. If one calls getAuthority
            // on the resultant URI, it will still return null.
            return new URI(svnProtocol, "", baseDir, null, null);

        } else {

            // Someone in Sun-land decided that NULL (no value) is the same thing as 0 (a value).
            // Fortunately 0 is not a valid port for any protocol we care about.
            if (port != 0) {
                return new URI(svnProtocol, null, host, port, baseDir, null, null);
            } else {
                return new URI(svnProtocol, host, baseDir, null);             
            }
        }
    }
}
