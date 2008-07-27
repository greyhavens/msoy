//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;

import com.threerings.msoy.data.all.MediaDesc;

public class FileItemUploadFile extends UploadFile
{
    public FileItemUploadFile (FileItem item)
        throws IOException
    {
        _item = item;
        _detectedMimeType = detectMimeType();
    }

    @Override // from UploadFile
    public InputStream getInputStream ()
        throws IOException
    {
        return _item.getInputStream();
    }

    @Override // from UploadFile
    public byte getMimeType ()
    {
        // look up the mime type from the item content type first
        byte mimeType = MediaDesc.stringToMimeType(_item.getContentType());
        if (mimeType != MediaDesc.INVALID_MIME_TYPE) {
            return mimeType;
        }
        return _detectedMimeType;
    }

    @Override // from UploadFile
    public String getOriginalName ()
    {
        // TODO: Supposedly Opera adds the full path when setting that field name for the
        // FileItem so we might need to sanitize this value
        return _item.getName();
    }

    protected FileItem _item;
}
