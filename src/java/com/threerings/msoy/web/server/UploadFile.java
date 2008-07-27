//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Wrap various file objects and provide useful methods for dealing with uploaded file data.
 */
public abstract class UploadFile
{
    /**
     * Returns the mime type detected from the wrapped file data.
     */
    public abstract byte getMimeType ();

    /**
     * Returns the mime type as a string.
     */
    public String getMimeTypeAsString ()
    {
        return MediaDesc.mimeTypeToString(getMimeType());
    }

    /**
     * Returns the hash generated from the file data.
     */
    // TODO: should not throw IOException
    public String getHash ()
        throws IOException
    {
        if (_hash == null) {
            _hash = generateHash();
        }
        return _hash;
    }

    /**
     * Returns an input stream from the file data wrapped by this UploadFile.
     */
    public abstract InputStream getInputStream () throws IOException;

    /**
     * Returns the name of the original file wrapped by this UploadFile.
     */
    public abstract String getOriginalName ();

    /**
     * Read data from a FileItem, calculating and returning a SHA hash.
     * @return the hash as hex in a string.
     * @throws RuntimeException if no SHA hash implementation could be found.
     */
    protected String generateHash ()
        throws IOException
    {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException(nsa.getMessage());
        }

        // read from the input stream and append to the digest, sending the bits to /dev/null
        InputStream digestIn = new DigestInputStream(getInputStream(), digest);
        OutputStream devOut = new NullOutputStream();
        try {
            IOUtils.copy(digestIn, devOut);

        } finally {
            StreamUtil.close(digestIn);
            StreamUtil.close(devOut);
        }

        // return the hash
        return StringUtil.hexlate(digest.digest());
    }

    /**
     * Determine the mime type of the FileItem.
     */
    protected byte detectMimeType ()
        throws IOException
    {
        byte mimeType;

        // try inferring the type from the file name
        mimeType = MediaDesc.suffixToMimeType(getOriginalName());
        if (mimeType != MediaDesc.INVALID_MIME_TYPE) {
            return mimeType;
        }

        // if we could not discern from the file path, try determining the mime type
        // from the file data itself.
        byte[] firstBytes = new byte[_mimeMagic.getMinArrayLength()];
        String mimeString = null;

        // Read identifying bytes from the uploaded file
        if (getInputStream().read(firstBytes, 0, firstBytes.length) < firstBytes.length) {
            return MediaDesc.INVALID_MIME_TYPE;
        }

        // Sufficient data was read, attempt magic identification
        mimeString = _mimeMagic.identify(firstBytes, getOriginalName(), null);

        if (mimeString != null) {
            // XXX debugging; want to know the effectiveness, any false hits,
            // and what types of files are being uploaded -- landonf (March 5, 2007)
            log.info("Magically determined unknown mime type [type=" + mimeString +
                     ", name=" + getOriginalName() + "].");
            return MediaDesc.stringToMimeType(mimeString);
        } else {
            return MediaDesc.INVALID_MIME_TYPE;
        }
    }

    protected byte _detectedMimeType;
    protected String _hash;

    /** A magic mime type identifier. */
    protected final MimeTypeIdentifier _mimeMagic = new MagicMimeTypeIdentifier();

    /** Used when generating the hash */
    protected static final int HASH_BUFFER_SIZE = 4096;
}
