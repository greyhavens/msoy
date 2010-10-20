//
// $Id: StaticMediaDesc.java 10116 2008-07-27 15:40:58Z mdb $

package com.threerings.msoy.data.all;

/**
 * Provides a "faked" media descriptor for media served internally at arbitrary relative URLs.
 * NOTE: Unless {@link MediaDesc} and {@link StaticMediaDesc}, this class does not exist in the
 * AS3 client, and must not be streamed there.
 */
public class InternalMediaDesc extends MediaDesc
{
    /** Used for unserialization. */
    public InternalMediaDesc ()
    {
    }

    /**
     * Creates a configured static media descriptor.
     */
    public InternalMediaDesc (String path, byte mimeType, String mediaType)
    {
        this(path, mimeType, mediaType, NOT_CONSTRAINED);
    }

    /**
     * Creates a configured static media descriptor.
     */
    public InternalMediaDesc (String path, byte mimeType, String mediaType, byte constraint)
    {
        super(mimeType, constraint);
        _path = path;
    }

    @Override // from MediaDesc
    public String getMediaPath ()
    {
        return DeploymentConfig.serverURL + _path + MediaMimeTypes.mimeTypeToSuffix(mimeType);
    }

    protected String _path;
}
