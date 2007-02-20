//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A digital item representing a simple text document.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="DOCUMENT")
public class DocumentRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #docMediaHash} field. */
    public static final String DOC_MEDIA_HASH = "docMediaHash";

    /** The qualified column identifier for the {@link #docMediaHash} field. */
    public static final ColumnExp DOC_MEDIA_HASH_C =
        new ColumnExp(DocumentRecord.class, DOC_MEDIA_HASH);

    /** The column identifier for the {@link #docMimeType} field. */
    public static final String DOC_MIME_TYPE = "docMimeType";

    /** The qualified column identifier for the {@link #docMimeType} field. */
    public static final ColumnExp DOC_MIME_TYPE_C =
        new ColumnExp(DocumentRecord.class, DOC_MIME_TYPE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the document media. */
    public byte[] docMediaHash;

    /** The MIME type of the {@link #docMediaHash} media. */
    public byte docMimeType;

    public DocumentRecord ()
    {
        super();
    }

    protected DocumentRecord (Document document)
    {
        super(document);

        if (document.docMedia != null) {
            docMediaHash = document.docMedia.hash;
            docMimeType = document.docMedia.mimeType;
        }
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.DOCUMENT;
    }

    @Override
    protected Item createItem ()
    {
        Document object = new Document();
        object.docMedia = docMediaHash == null ? null :
            new MediaDesc(docMediaHash, docMimeType);
        return object;
    }
}
