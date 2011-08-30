//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.common.collect.ComparisonChain;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * A fully qualified catalog record identifier (type and integer id).
 */
public class CatalogIdent
    implements Comparable<CatalogIdent>, Streamable, IsSerializable
{
    /** The item type identifier. */
    public MsoyItemType type;

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
    public CatalogIdent (MsoyItemType type, int catalogId)
    {
        this.type = type;
        this.catalogId = catalogId;
    }

    // from Comparable
    public int compareTo (CatalogIdent that)
    {
        return ComparisonChain.start()
                .compare(this.type, that.type)
                .compare(this.catalogId, that.catalogId).
                result();
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
        return (type.toByte() * 37) | catalogId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return type + ":" + catalogId;
    }
}
