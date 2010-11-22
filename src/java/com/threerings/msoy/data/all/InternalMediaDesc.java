//
// $Id: StaticMediaDesc.java 10116 2008-07-27 15:40:58Z mdb $

package com.threerings.msoy.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Provides a "faked" media descriptor for media served internally at arbitrary relative URLs.
 * NOTE: Unless {@link MediaDesc} and {@link StaticMediaDesc}, this class does not exist in the
 * AS3 client, and must not be streamed there.
 */
public class InternalMediaDesc extends BasicMediaDesc
{
    /** Used for unserialization. */
    public InternalMediaDesc ()
    {
    }

    /**
     * Creates a configured static media descriptor.
     */
    public InternalMediaDesc (String path, byte mimeType)
    {
        this(path, mimeType, NOT_CONSTRAINED);
    }

    /**
     * Creates a configured static media descriptor.
     */
    public InternalMediaDesc (String path, byte mimeType, byte constraint)
    {
        super(mimeType, constraint);
        _path = path;
    }

    // from MediaDesc
    public MediaDesc newWithConstraint (byte constraint)
    {
        return new InternalMediaDesc(_path, getMimeType(), constraint);
    }

    public String getMediaPath ()
    {
        return DeploymentConfig.serverURL + _path + MediaMimeTypes.mimeTypeToSuffix(getMimeType());
    }

    protected String _path;
}
