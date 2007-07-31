//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.net.URI;
import java.net.URISyntaxException;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.swiftly.server.storage.ProjectSVNStorage;

/**
 * Contains the definition of a swiftly svn-based project storage.
 * 
 * BEWARE: In a number of locations we artifically constrain the size
 * of a string column in order to work around the following bug:
 *    http://bugs.mysql.com/bug.php?id=4541
 * In short, MySQL doesn't support unique keys composed of values potentially
 * greater than 1000 bytes. Quick math:
 *    VARCHAR(255) * (UTF-8 4 bytes) == 1020
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(fieldNames={"protocol", "host", "port", "baseDir"})})
public class SwiftlySVNStorageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #storageId} field. */
    public static final String STORAGE_ID = "storageId";

    /** The qualified column identifier for the {@link #storageId} field. */
    public static final ColumnExp STORAGE_ID_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, STORAGE_ID);

    /** The column identifier for the {@link #protocol} field. */
    public static final String PROTOCOL = "protocol";

    /** The qualified column identifier for the {@link #protocol} field. */
    public static final ColumnExp PROTOCOL_C =
        new ColumnExp(SwiftlySVNStorageRecord.class, PROTOCOL);

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

    public static final int SCHEMA_VERSION = 4;

    /** The id of the project. */
    @Id
    @GeneratedValue
    public int storageId;

    /** The SVN protocol (svn+ssh, svn, https, etc). */
    @Column(length=10)
    public String protocol;

    /** The storage host (FQDN). */
    @Column(length=128)
    public String host;

    /** The storage port number. */
    public int port;

    /** The storage base directory. */
    @Column(length=100)
    public String baseDir;
    
    public URI toURI ()
        throws URISyntaxException
    {        
        if (protocol.equals(ProjectSVNStorage.PROTOCOL_FILE)) {
            // Confusingly (buggily?) one must set an empty authority in order for URI
            // to properly append '//' to the file: scheme. If one calls getAuthority
            // on the resultant URI, it will still return null.
            return new URI(protocol, "", baseDir, null, null);

        } else {

            // Someone in Sun-land decided that NULL (no value) is the same thing as 0 (a value).
            // Fortunately 0 is not a valid port for any protocol we care about.
            if (port != 0) {
                return new URI(protocol, null, host, port, baseDir, null, null);
            } else {
                return new URI(protocol, host, baseDir, null);             
            }
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #SwiftlySVNStorageRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlySVNStorageRecord> getKey (int storageId)
    {
        return new Key<SwiftlySVNStorageRecord>(
                SwiftlySVNStorageRecord.class,
                new String[] { STORAGE_ID },
                new Comparable[] { storageId });
    }
    // AUTO-GENERATED: METHODS END
}
