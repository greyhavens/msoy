/**
 * 
 */
package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Wrap a FileItem and provide useful methods for dealing with uploaded file data.
 */
public class UploadFile
{
    public final FileItem item;
    
    public UploadFile (FileItem item)
        throws IOException
    {
        this.item = item;
        _mimeType = detectMimeType();
    }
    
    /**
     * Returns the mime type detected from the FileItem data.
     */
    public byte getMimeType ()
    {
        return _mimeType;
    }
    
    /**
     * Returns the mime type detected from the FileItem data.
     */
    public String getMimeTypeAsString ()
    {
        return MediaDesc.mimeTypeToString(_mimeType);
    }

    /**
     * Read data from a FileItem, calculating and returning a SHA hash.
     * @return the hash as hex in a string.
     * @throws RuntimeException if no SHA hash implementation could be found.
     */
    public String generateHash ()
        throws IOException
    {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException(nsa.getMessage());
        }

        // read from the input stream and append to the digest, sending the bits to /dev/null
        InputStream digestIn = new DigestInputStream(item.getInputStream(), digest);
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
        
        // look up the mime type
        mimeType = MediaDesc.stringToMimeType(item.getContentType());
        if (mimeType != MediaDesc.INVALID_MIME_TYPE) {
            return mimeType;
        }

        // if that failed, try inferring the type from the path
        mimeType = MediaDesc.suffixToMimeType(item.getName());
        if (mimeType != MediaDesc.INVALID_MIME_TYPE) {
            return mimeType;
        }

        // if we could not discern from the file path, try determining the mime type
        // from the file data itself.
        byte[] firstBytes = new byte[_mimeMagic.getMinArrayLength()];
        String mimeString = null;

        // Read identifying bytes from the uploaded file
        if (item.getInputStream().read(firstBytes, 0, firstBytes.length) < firstBytes.length) {
            return MediaDesc.INVALID_MIME_TYPE;
        }
        
        // Sufficient data was read, attempt magic identification
        mimeString = _mimeMagic.identify(firstBytes, item.getName(), null);

        if (mimeString != null) {
            // XXX debugging; want to know the effectiveness, any false hits,
            // and what types of files are being uploaded -- landonf (March 5, 2007)
            log.warning("Magically determined unknown mime type [type=" + mimeString +
                ", name=" + item.getName() + "].");
        }
        return MediaDesc.stringToMimeType(mimeString);     
    }

 
    protected byte _mimeType;
    protected String _hash;

    /** A magic mime type identifier. */
    protected final MimeTypeIdentifier _mimeMagic = new MagicMimeTypeIdentifier();

    /** Used when generating the hash */
    protected static final int HASH_BUFFER_SIZE = 4096;
}
