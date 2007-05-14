//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.openrdf.model.URI;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Detects file mimetypes.
 */
public class SwiftlyMimeTypeIdentifier
    implements MimeTypeIdentifier
{
    public SwiftlyMimeTypeIdentifier ()
    {
        _identifier = new MagicMimeTypeIdentifier();
    }

    // from MimeTypeIdentifier
    public int getMinArrayLength ()
    {
        return _identifier.getMinArrayLength();
    }

    // from MimeTypeIdentifier
    public String identify (byte[] firstBytes, String fileName, URI uri)
    {
        return _identifier.identify(firstBytes, fileName, uri);
    }

    /** Determines the mime type for the supplied file. Returns null if not found. */
    public String determineMimeType (String fileName, File fileData)
        throws IOException
    {
        String mimeType = null;
        FileInputStream stream = new FileInputStream(fileData);
        byte[] firstBytes = new byte[getMinArrayLength()];

        // Read identifying bytes from the to-be-added file
        if (stream.read(firstBytes, 0, firstBytes.length) >= firstBytes.length) {
            // Required data was read, attempt magic identification
            mimeType = identify(firstBytes, fileName, null);
        }

        // If that failed, try our internal path-based type detection.
        if (mimeType == null) {
            // Get the miserly byte mime-type
            byte miserMimeType = MediaDesc.suffixToMimeType(fileName);

            // If a valid type was returned, convert to a string.
            // Otherwise, don't set a mime type.
            // TODO: binary file detection
            if (miserMimeType != -1) {
                mimeType = MediaDesc.mimeTypeToString(miserMimeType);
            }
        }

        return mimeType;
    }

    protected MimeTypeIdentifier _identifier;

}
