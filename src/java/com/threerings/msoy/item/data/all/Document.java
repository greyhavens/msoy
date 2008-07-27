//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * A digital item representing a simple text document.
 */
public class Document extends Item
{
    /** The document media. */
    public MediaDesc docMedia;

    @Override // from Item
    public byte getType ()
    {
        return DOCUMENT;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (docMedia != null) && nonBlank(name, MAX_NAME_LENGTH);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia();
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return docMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        docMedia = desc;
    }
}
