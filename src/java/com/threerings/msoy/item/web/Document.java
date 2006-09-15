//
// $Id$

package com.threerings.msoy.item.web;

/**
 * A digital item representing a simple text document.
 */
public class Document extends Item
{
    /** A hash code identifying the document media. */
    public byte[] docMediaHash;

    /** The MIME type of the {@link #docMediaHash} media. */
    public byte docMimeType;

    /** The title of this document (max length 255 characters). */
    public String title;

    /**
     * Returns a media descriptor for the actual document media.
     */
    public MediaDesc getDocumentMedia ()
    {
        return new MediaDesc(docMediaHash, docMimeType);
    }

    // @Override from Item
    public String getType ()
    {
        return "DOCUMENT";
    }

    // @Override from Item
    public String getDescription ()
    {
        return title;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (docMediaHash != null) &&
            nonBlank(title);
    }
}
