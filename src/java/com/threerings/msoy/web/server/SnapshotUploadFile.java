//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;

import com.threerings.msoy.item.data.all.MediaDesc;

public class SnapshotUploadFile extends FileItemUploadFile
{
    public SnapshotUploadFile (FileItem item, int sceneId)
        throws IOException
    {
        super(item);
        _sceneId = sceneId;
    }
    
    public int getSceneId ()
    {
        return _sceneId;
    }

    protected int _sceneId;
}
