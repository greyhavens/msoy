//
// $Id$
package com.threerings.msoy.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GenericUploadFile extends UploadFile
{
    public GenericUploadFile (File file)
        throws IOException
    {
        _file = file;
        _detectedMimeType = detectMimeType();
    }

    @Override // from UploadFile
    public InputStream getInputStream ()
        throws IOException
    {
        return new FileInputStream(_file);
    }

    @Override // from UploadFile
    public byte getMimeType ()
    {
        return _detectedMimeType;
    }

    @Override // from UploadFile
    public String getOriginalName ()
    {
        return _file.getName();
    }

    protected File _file;
}
