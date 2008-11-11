//
// $Id$

package com.threerings.msoy.stuff.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.threerings.msoy.web.server.UploadFile;

import com.threerings.msoy.data.all.MediaDesc;

public class ExternalUploadFile extends UploadFile
{
    public ExternalUploadFile (String data, byte mimeType)
    {
        _data = data;
        _detectedMimeType = mimeType;
    }

    @Override
    public InputStream getInputStream ()
        throws IOException
    {
        return new ByteArrayInputStream(_data.getBytes());
    }

    @Override
    public byte getMimeType ()
    {
        return _detectedMimeType;
    }

    @Override
    public String getOriginalName ()
    {
        return "NotApplicable";
    }

    protected String _data;
}
