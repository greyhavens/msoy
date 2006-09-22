//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Item;

/**
 * A digital item representing a simple text document.
 */
@Entity
@Table
@TableGenerator(
    name="itemId",
    allocationSize=1,
    pkColumnValue="DOCUMENT")
public class DocumentRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;
    
    public static final String DOC_MEDIA_HASH = "docMediaHash";
    public static final String DOC_MIME_TYPE = "docMimeType";
    public static final String TITLE = "title";
    
    /** A hash code identifying the document media. */
    @Column(nullable=false)
    public byte[] docMediaHash;

    /** The MIME type of the {@link #docMediaHash} media. */
    @Column(nullable=false)
    public byte docMimeType;

    /** The title of this document (max length 255 characters). */
    @Column(nullable=false)
    public String title;
    
    public DocumentRecord ()
    {
        super();
    }

    protected DocumentRecord (Document document)
    {
        super(document);

        this.docMediaHash = document.docMediaHash == null ?
            null : document.docMediaHash.clone();
        this.docMimeType = document.docMimeType;
        this.title = document.title;
    }

    @Override // from ItemRecord
    public ItemEnum getType ()
    {
        return ItemEnum.DOCUMENT;
    }

    @Override
    public Object clone ()
    {
        DocumentRecord clone = (DocumentRecord) super.clone();
        clone.docMediaHash = docMediaHash.clone();
        return clone;
    }

    @Override
    protected Item createItem ()
    {
        Document object = new Document();
        object.docMediaHash = this.docMediaHash == null ?
            null : this.docMediaHash.clone();
        object.docMimeType = this.docMimeType;
        object.title = this.title;
        return object;
    }
}
