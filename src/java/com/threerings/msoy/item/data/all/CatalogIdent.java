//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.io.Streamable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A fully qualified catalog record identifier (type and integer id).
 */
public class CatalogIdent
    implements Comparable<CatalogIdent>, Streamable, IsSerializable
{
    /** The item type identifier. */
    public byte type;

    /** The integer identifier of the catalog entry. */
    public int catalogId;

    /**
     * A constructor used for unserialization.
     */
    public CatalogIdent ()
    {
    }

    /**
     * Creates an identifier for the specified catalog entry.
     */
    public CatalogIdent (byte type, int catalogId)
    {
        this.type = type;
        this.catalogId = catalogId;
    }

    // from Comparable
    public int compareTo (CatalogIdent that)
    {
        // first, compare by type.
        if (this.type < that.type) {
            return 1;

        } else if (this.type > that.type) {
            return -1;

        } else {
            // if type is equal, compare by item id
            if (this.catalogId < that.catalogId) {
                return 1;

            } else if (this.catalogId > that.catalogId) {
                return -1;

            } else {
                return 0;
            }
        }
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof CatalogIdent) {
            CatalogIdent that = (CatalogIdent) other;
            return (this.type == that.type) && (this.catalogId == that.catalogId);
        }
        return false;
    }

    @Override // from Object
    public int hashCode ()
    {
        return (type * 37) | catalogId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return type + ":" + catalogId;
    }
}
