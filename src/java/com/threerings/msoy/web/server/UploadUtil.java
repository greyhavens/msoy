//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.collect.ImmutableMap;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.world.server.SnapshotUploadFile;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3FileObject;
import com.threerings.s3.client.S3ServerException;
import com.threerings.s3.client.acl.AccessControlList;

/**
 * Contains routines for receiving and publishing uploaded media files.
 */
public class UploadUtil
{
    /**
     * A data class used when generating the item javascript.
     */
    public static class MediaInfo
    {
        public final String hash;
        public final byte mimeType;
        public final byte constraint;
        public final int width;
        public final int height;

        // a "blank" thumbnail
        public MediaInfo ()
        {
            this("", (byte)0);
        }

        // for non image data
        public MediaInfo (String hash, byte mimeType)
        {
            this(hash, mimeType, (byte)0, 0, 0);
        }

        // for image data
        public MediaInfo (String hash, byte mimeType, byte constraint, int width, int height)
        {
            this.hash = hash;
            this.mimeType = mimeType;
            this.constraint = constraint;
            this.width = width;
            this.height = height;
        }

        // for thumbnails
        public MediaInfo (MediaInfo other, byte constraint)
        {
            hash = other.hash;
            mimeType = other.mimeType;
            this.constraint = constraint;
            width = other.width;
            height = other.height;
        }

        @Override
        public String toString () {
            return "MediaInfo: hash=" + hash + ", mimeType=" + mimeType + ", constraint="
                + constraint + ", width=" + width + ", height=" + height;
        }
    }

    /**
     * Utility class to encapsulate the hash / digest creation process which is used at multiple
     * points in this method.
     */
    protected static class Digester 
    {        
        public MessageDigest digest;

        public Digester () 
        {
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsa) {
                throw new RuntimeException(nsa.getMessage());
            }
        }
        
        /**
         * Provides an output stream to write the data to be digested to.
         */
        public OutputStream getOutputStream () 
        {
            _bout = new ByteArrayOutputStream();
            final DigestOutputStream digout = new DigestOutputStream(_bout, digest);
            return digout;
        }

        /**
         * Convenience method to drain and digest the contents of an input stream. After a call to
         * this method, the digest is ready and the data can be read again from the outputstream
         * of the digester.
         */
        public void digest (InputStream inputStream) 
            throws IOException
        {         
            IOUtils.copy(inputStream, getOutputStream());
            close();
        }
        
        /**
         * Closes the output stream used to pass data into the digester, finalizing the data.
         */
        public void close () 
            throws IOException 
        {
            if (_bout != null) {
                _bout.close();                
            }
        }
        
        /**
         * Provides an input stream for the digested data, which should be identical to the data
         * that was written to the output stream.
         */
        public InputStream getInputStream () 
        {
            final ByteArrayInputStream bin = new ByteArrayInputStream(_bout.toByteArray());
            return bin;
        }
        
        public String getHash() 
        {
            return StringUtil.hexlate(digest.digest());
        }
        
        protected ByteArrayOutputStream _bout;
    }
    
    /**
     * Publishes an InputStream to the default media store location, using the hash and the
     * mimeType of the stream.
     */
    public static void publishStreamAsHash (InputStream input, String hash, byte mimeType)
        throws IOException
    {
        // name it using the hash value and the suffix
        String name = hash + MediaDesc.mimeTypeToSuffix(mimeType);
        publishStream(input, null, name, MediaDesc.mimeTypeToString(mimeType), mimeType,
            EXPIRES_2038);
    }

    /**
     * Publishes an InputStream to the media store at the specified subdirectory location, using
     * name of the stream (usually its hash) and a supplied mime type. Currently this is to the
     * filesystem first, and then s3 if enabled. A map of headers to be added to the s3 object may
     * be supplied.
     * 
     * @return MediaInfo Note that the dimensions and constraint will be zero in the returned
     * MediaInfo object because the image is not decoded.
     */
    protected static void publishStream (InputStream input, String subdirectory, String name,
                                      String mimeType, byte mime, Map<String,String> headers)
        throws IOException
    {
        // copy the uploaded file data to the local file system media store. eventually we will
        // only be keeping a local file on developer's machines        
        final File target = copyStreamToFile(input, subdirectory, name);

        // ALSO publish to s3 if enabled
        // TODO: check if this hash is already in the media store and skip the upload if found
        if (ServerConfig.mediaS3Enable) {
            copyFileToS3(target, mimeType, name, subdirectory, headers);
        }
    }

    /**
     * Copy the supplied input stream to a file in the specified subdirectory with the specified
     * name.  If null is supplied as the subdirectory, a default is used, and if the subdirectory
     * specified does not exist, an attempt is made to create it.
     */
    private static File copyStreamToFile (InputStream input, String subdirectory, String name)
        throws IOException, FileNotFoundException
    {
        File location = ServerConfig.mediaDir; // default media directory

        // if a subdirectory was specified, set that as the new location, and make sure it exists
        if (subdirectory != null) {
            location = new File(ServerConfig.mediaDir, subdirectory);
            if (! location.exists()) {
                location.mkdir();
            }
        }

        // save to our new file location
        File target = new File(location, name);
        IOUtils.copy(input, new FileOutputStream(target));
        return target;
    }

    /**
     * Copy the supplied file to S3
     */
    protected static boolean copyFileToS3 (
        File file, String mimeType, String name, String subdirectory, Map<String, String> headers)
        throws IOException
    {
        try {
            S3Connection conn = new S3Connection(
                ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);
            S3FileObject uploadTarget = new S3FileObject(name, file, mimeType);

            if (headers == null ) {
                headers = Collections.emptyMap();
            }

            conn.putObject(ServerConfig.mediaS3Bucket, uploadTarget,
                           AccessControlList.StandardPolicy.PUBLIC_READ, headers);

            log.info("Uploaded media to S3 [bucket=" + ServerConfig.mediaS3Bucket +
                     ", name=" + name + "].");
            return true;

        } catch (S3ServerException e) {
            // S3 Server-side Exception
            log.warning("S3 upload failed [code=" + e.getClass().getName() +
                        ", requestId=" + e.getRequestId() + ", hostId=" + e.getHostId() +
                        ", message=" + e.getMessage() + "].");
            return false;

        } catch (S3Exception e) {
            // S3 Client-side Exception
            log.warning("S3 upload failed: " + e);
            return false;
        }
    }

    /**
     * Publishes a canonical scene snapshot, saving both a standard size image, and a reduced size
     * thumbnail.  Both are saved under their respective hashes.
     */
    public static void publishSnapshot (SnapshotUploadFile uploadFile)
        throws IOException
    {
        log.warning("publish snapshot called");

        // publish the regular sized image
        MediaInfo canonicalInfo = publishCanonicalImage(uploadFile);

        // publish a reduced sized version of the image
        MediaInfo thumbInfo =
            publishImage(MediaDesc.SNAPSHOT_THUMB_SIZE, uploadFile,
            uploadFile.getMimeType(), "jpg");

        log.info("MediaInfo for canonical is: "+canonicalInfo);
        log.info("MediaInfo for thumbnail is: "+thumbInfo);
    }

    protected static MediaInfo publishCanonicalImage (SnapshotUploadFile uploadFile) 
        throws IOException 
    {
        publishStreamAsHash(uploadFile.getInputStream(), uploadFile.getHash(),
            uploadFile.getMimeType());
        return new MediaInfo(uploadFile.getHash(), uploadFile.getMimeType(), (byte) 0, 0, 0);
    }
    
    /**
     * Publishes an UploadFile to the media store.
     */
    public static void publishUploadFile (UploadFile uploadFile)
        throws IOException
    {
        publishStreamAsHash(uploadFile.getInputStream(), uploadFile.getHash(),
            uploadFile.getMimeType());
    }

    /**
     * Computes and fills in the constraints on the supplied image, scaling thumbnails as
     * necessary, and publishes the image data to the media store.
     *
     * @return a MediaInfo object filled in with the published image info.
     */
    public static MediaInfo publishImage (String mediaId, UploadFile uploadFile)
        throws IOException
    {
        int size = Item.THUMB_MEDIA.equals(mediaId)
            ? MediaDesc.THUMBNAIL_SIZE : MediaDesc.PREVIEW_SIZE;
        return publishImage(size, uploadFile, THUMBNAIL_MIME_TYPE, THUMBNAIL_IMAGE_FORMAT);
    }

    /**
     * Computes and fills in the constraints on the supplied image, scaling thumbnails as
     * necessary, and publishes the image data to the media store.
     *
     * @param size the size of the thumbnail to generate, or previewSize to
     * not generate a thumbnail. Wacky, I know.
     *
     * @return a MediaInfo object filled in with the published image info.
     */
     public static MediaInfo publishImage (
         Integer thumbSize, UploadFile uploadFile, byte thumbMime, String thumbFormat)
         throws IOException
     {
         // convert the uploaded file data into an image object
         final BufferedImage image = ImageIO.read(uploadFile.getInputStream());
         if (image == null) {
             throw new IOException("Invalid image data. Unable to complete upload.");
         }

         String hash = uploadFile.getHash();
         byte mimeType = uploadFile.getMimeType();
         int width = image.getWidth();
         int height = image.getHeight();

         // if we're uploading a thumbnail image...
         boolean published = false;
         if (thumbSize != null) {
             final int targetWidth = MediaDesc.DIMENSIONS[thumbSize * 2];
             final int targetHeight = MediaDesc.DIMENSIONS[thumbSize * 2 + 1];
             // ...and we may need to scale our image
             if (width > targetWidth || height > targetHeight) {
                 // determine the size of our to be scaled image
                 final float tratio = targetWidth / (float)targetHeight;
                 final float iratio = image.getWidth() / (float)image.getHeight();
                 final float scale = iratio > tratio ?
                     targetWidth / (float)image.getWidth() :
                     targetHeight / (float)image.getHeight();
                 width = Math.max(1, Math.round(scale * image.getWidth()));
                 height = Math.max(1, Math.round(scale * image.getHeight()));

                 // generate the scaled image
                 final BufferedImage timage =
                     new BufferedImage(width, height, samplingModelForFormat(thumbFormat));
                 final Graphics2D gfx = timage.createGraphics();
                 try {
                     gfx.drawImage(image, 0, 0, width, height, null);
                 } finally {
                     gfx.dispose();
                 }

                 mimeType = thumbMime;

                 Digester digester = new Digester();
                 ImageIO.write(timage, thumbFormat, 
                     new MemoryCacheImageOutputStream(digester.getOutputStream()));
                 digester.close();

                 hash = digester.getHash();
                 
                 // publish the thumbnail
                 publishStreamAsHash(digester.getInputStream(), hash, mimeType);

                 published = true;
             }
         }

         // if we haven't yet published our image, do so
         // (this happens when we didn't need to scale the image to produce a thumbnail)
         if (!published) {
             publishUploadFile(uploadFile);
         }

         // finally compute our constraint and return a media info
         // todo: this is broken if thumbsize is null
         int constraintSize = thumbSize == null ? MediaDesc.PREVIEW_SIZE : thumbSize.intValue();
         byte constraint = MediaDesc.computeConstraint(constraintSize, width, height);

         return new MediaInfo(hash, mimeType, constraint, width, height);
     }

    /**
     * Return the sampling model that should be used for rendering a given image prior to encoding
     * in the given format.
     *
     * (javax.imageio.ImageIO doesn't work properly if you use a sampling mode that includes an
     * alpha channel when writing a format that doesn't support alpha.)
     */
    protected static int samplingModelForFormat (String informalName)
        throws IOException
    {
        final Integer found = samplingModels.get(informalName.toLowerCase());
        if (found == null) {
            throw new IOException("image sampling model not known for format: "+informalName);
        }
        return found;
    }

    /**
     * Mapping of informal image format names to buffer sample mode.
     */
    protected static final Map<String, Integer> samplingModels = ImmutableMap.of(
            "jpg", BufferedImage.TYPE_INT_RGB,
            "jpeg", BufferedImage.TYPE_INT_RGB,
            "png", BufferedImage.TYPE_INT_ARGB,
            "tiff", BufferedImage.TYPE_INT_ARGB
        );

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";

    // Effectively 'never' expire date.
    protected static final Map<String,String> EXPIRES_2038 =
        ImmutableMap.of("Expires", "Sun, 17 Jan 2038 19:14:07 GMT");
}
