//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import java.net.URI;
import java.net.URISyntaxException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;
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
@Entity(uniqueConstraints={
    @UniqueConstraint(name="phpb", fields={"protocol", "host", "port", "baseDir"})
})
public class SwiftlySVNStorageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SwiftlySVNStorageRecord> _R = SwiftlySVNStorageRecord.class;
    public static final ColumnExp STORAGE_ID = colexp(_R, "storageId");
    public static final ColumnExp PROTOCOL = colexp(_R, "protocol");
    public static final ColumnExp HOST = colexp(_R, "host");
    public static final ColumnExp PORT = colexp(_R, "port");
    public static final ColumnExp BASE_DIR = colexp(_R, "baseDir");
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
     * Create and return a primary {@link Key} to identify a {@link SwiftlySVNStorageRecord}
     * with the supplied key values.
     */
    public static Key<SwiftlySVNStorageRecord> getKey (int storageId)
    {
        return new Key<SwiftlySVNStorageRecord>(
                SwiftlySVNStorageRecord.class,
                new ColumnExp[] { STORAGE_ID },
                new Comparable[] { storageId });
    }
    // AUTO-GENERATED: METHODS END
}
