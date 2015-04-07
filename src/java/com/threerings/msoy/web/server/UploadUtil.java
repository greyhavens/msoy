//
// $Id$

package com.threerings.msoy.web.server;

import java.awt.Graphics2D;
import java.awt.Image;
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

import com.google.common.collect.ImmutableMap;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.samskivert.util.StringUtil;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.s3.client.MediaType;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3FileObject;
import com.threerings.s3.client.S3ServerException;
import com.threerings.s3.client.acl.AccessControlList;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaDescUtil;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.room.server.SnapshotUploadFile;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

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
        public MediaInfo () {
            this("", (byte)0);
        }

        // for non image data
        public MediaInfo (String hash, byte mimeType) {
            this(hash, mimeType, (byte)0, 0, 0);
        }

        // for image data
        public MediaInfo (String hash, byte mimeType, byte constraint, int width, int height) {
            this.hash = hash;
            this.mimeType = mimeType;
            this.constraint = constraint;
            this.width = width;
            this.height = height;
        }

        // for thumbnails
        public MediaInfo (MediaInfo other, byte constraint) {
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

    public static class SnapshotInfo
    {
        public final byte[] hash;
        public final byte type;

        public SnapshotInfo (byte[] hash, byte type) {
            this.hash = hash;
            this.type = type;
        }

        @Override
        public String toString () {
            return "SnapshotInfo: hash=" + hash + ", mimeType = " + type;
        }
    }

    /**
     * Container class for information about the canonical snapshot images - both standard size
     * and minimized.
     */

    public static class CanonicalSnapshotInfo
    {
        public final SnapshotInfo canonical;
        public final SnapshotInfo thumbnail;

        public CanonicalSnapshotInfo (SnapshotInfo canonical, SnapshotInfo thumbnail) {
            this.canonical = canonical;
            this.thumbnail = thumbnail;
        }
    }

    /**
     * Simple class to represent a 2 dimensional size.
     */
    protected static class Rectangle
    {
        public final int width;
        public final int height;

        /**
         * Create a dimensions object from integer sizes.
         * @param width the width
         * @param height the height
         */
        public Rectangle (int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Create a dimensions object from a buffered image.
         */
        public Rectangle (BufferedImage image) {
            this.width = image.getWidth();
            this.height = image.getHeight();
        }

        /**
         * Returns true if this dimension covers the one passed in.
         */
        public boolean covers (Rectangle d) {
            return (this.width >= d.width && this.height >= d.height);
        }
    }

    /**
     * Utility class to encapsulate the hash / digest creation process which is used at multiple
     * points in this method.
     */
    protected static class Digester
    {
        public Digester () {
            try {
                _digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsa) {
                throw new RuntimeException(nsa.getMessage());
            }
        }

        /**
         * Provides an output stream to write the data to be digested to.
         */
        public OutputStream getOutputStream () {
            _bout = new ByteArrayOutputStream();
            return new DigestOutputStream(_bout, _digest);
        }

        /**
         * Convenience method to drain and digest the contents of an input stream. After a call to
         * this method, the digest is ready and the data can be read again from the outputstream
         * of the digester.
         */
        public void digest (InputStream inputStream) throws IOException {
            IOUtils.copy(inputStream, getOutputStream());
            close();
        }

        /**
         * Closes the output stream used to pass data into the digester, finalizing the data.
         */
        public void close () throws IOException {
            if (_bout != null) {
                _bout.close();
            }
        }

        /**
         * Provides an input stream for the digested data, which should be identical to the data
         * that was written to the output stream.
         */
        public InputStream getInputStream () {
            return new ByteArrayInputStream(_bout.toByteArray());
        }

        /**
         * Return the hash as a hexadecimal string.
         */
        public String hexHash () {
            if (_hex == null) {
                _hex = StringUtil.hexlate(binaryHash());
            }
            return _hex;
        }

        /**
         * Return the raw bytes of the hash.
         */
        public byte[] binaryHash () {
            if (_binary == null) {
                _binary = _digest.digest();
                _digest = null;
            }
            return _binary;
        }

        protected ByteArrayOutputStream _bout;
        protected MessageDigest _digest;
        protected byte[] _binary;
        protected String _hex;
    }

    /**
     * Intermediate representation of the results of publishing an image. Used to convert the
     * results into various formats required by different consumers.
     */
    public static class PublishingResult
    {
        public final Integer thumbSize;
        public final Rectangle originalSize;
        public final Rectangle finalSize;
        public final byte mimeType;
        public final Digester digester;

        /**
         * Create a publishing result.
         *
         * @param originalSize The original size of the image.
         * @param finalSize The final size of the image - will be the same as the original if the
         * image has not been resized.
         * @param thumbSize The integer thumbnail size number from MediaDesc- not a pixel size.
         * @param mimeType The mime-type of the final image.
         * @param digester The digester used to compute the hash of what's been uploaded.
         */
        public PublishingResult (Rectangle originalSize, Rectangle finalSize, Integer thumbSize,
                byte mimeType, Digester digester) {
            this.thumbSize = thumbSize;
            this.originalSize = originalSize;
            this.finalSize = finalSize;
            this.mimeType = mimeType;
            this.digester = digester;
        }

        /**
         * Convert the result into a MediaInfo object.
         */
        public MediaInfo getMediaInfo () {
            return new MediaInfo(digester.hexHash(), mimeType, getConstraint(), finalSize.width,
                finalSize.height);
        }

        public MediaDesc getMediaDesc () {
            return new HashMediaDesc(digester.binaryHash(), mimeType, getConstraint());
        }

        /**
         * Convert the result into a SnapshotInfo object.
         */
        public SnapshotInfo getSnapshotInfo () {
            return new SnapshotInfo(digester.binaryHash(), mimeType);
        }

        protected byte getConstraint ()
        {
            int constraintSize = thumbSize == null ?
                MediaDescSize.PREVIEW_SIZE : thumbSize.intValue();
            return MediaDescUtil.computeConstraint(
                constraintSize, originalSize.width, originalSize.height);
        }
    }

    /**
     * Publishes an InputStream to the default media store location, using the hash and the
     * mimeType of the stream.
     */
    protected static void publishStreamAsHash (InputStream input, String hash, byte mimeType)
        throws IOException
    {
        // name it using the hash value and the suffix
        String name = hash + MediaMimeTypes.mimeTypeToSuffix(mimeType);
        publishStream(input, null, name, MediaMimeTypes.mimeTypeToString(mimeType), mimeType,
            EXPIRES_2038);
    }

    /**
     * Publishes an InputStream to the media store at the specified subdirectory location, using
     * name of the stream (usually its hash) and a supplied mime type. Currently this is to the
     * filesystem first, and then s3 if enabled. A map of headers to be added to the s3 object may
     * be supplied.
     */
    protected static void publishStream (InputStream input, String subdirectory, String name,
        String mimeType, byte mime, Map<String, String> headers)
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
     * name. If null is supplied as the subdirectory, a default is used, and if the subdirectory
     * specified does not exist, an attempt is made to create it.
     */
    private static File copyStreamToFile (InputStream input, String subdirectory, String name)
        throws IOException, FileNotFoundException
    {
        File location = ServerConfig.mediaDir; // default media directory

        // if a subdirectory was specified, set that as the new location, and make sure it exists
        if (subdirectory != null) {
            location = new File(ServerConfig.mediaDir, subdirectory);
            if (!location.exists()) {
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
    protected static boolean copyFileToS3 (File file, String mimeType, String name,
        String subdirectory, Map<String, String> headers)
        throws IOException
    {
        try {
            HostConfiguration host = new HostConfiguration();
            host.setHost("s3-us-west-2.amazonaws.com", 80, "http");
            S3Connection conn = new S3Connection(ServerConfig.mediaS3Id, ServerConfig.mediaS3Key, host);
            S3FileObject uploadTarget = new S3FileObject(name, file, new MediaType(mimeType));

            if (headers == null) {
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
     * thumbnail. Both are saved under their respective hashes.
     */
    public static CanonicalSnapshotInfo publishSnapshot (SnapshotUploadFile uploadFile)
        throws IOException
    {
        // publish the regular sized image
        SnapshotInfo canonicalInfo = publishCanonicalImage(uploadFile);

        // publish a reduced sized version of the image
        SnapshotInfo thumbInfo = publishImage(MediaDescSize.SNAPSHOT_THUMB_SIZE, uploadFile,
            uploadFile.getMimeType(), "jpg").getSnapshotInfo();

        return new CanonicalSnapshotInfo(canonicalInfo, thumbInfo);
    }

    /**
     * Publish the canonical image at it's predefined size.
     */
    protected static SnapshotInfo publishCanonicalImage (SnapshotUploadFile uploadFile)
        throws IOException
    {
        Digester digester = new Digester();
        digester.digest(uploadFile.getInputStream());

        publishStreamAsHash(digester.getInputStream(), digester.hexHash(),
            uploadFile.getMimeType());

        return new SnapshotInfo(digester.binaryHash(), uploadFile.getMimeType());
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
     * necessary, and publishes the image data to the media store. Furni media will be scaled to
     * Preview size, as used by Photos in Galleries and shop preview panes.
     *
     * @return a MediaInfo object filled in with the published image info.
     */
    public static MediaInfo publishImage (String mediaId, UploadFile uploadFile,
        boolean scaleFurni)
        throws IOException
    {
        Integer size = null;
        if (Item.THUMB_MEDIA.equals(mediaId)) {
            size = MediaDescSize.THUMBNAIL_SIZE;
        } else if (scaleFurni && Item.FURNI_MEDIA.equals(mediaId)) {
            size = MediaDescSize.PREVIEW_SIZE;
        }
        return publishImage(size, uploadFile, THUMBNAIL_MIME_TYPE, THUMBNAIL_IMAGE_FORMAT
            ).getMediaInfo();
    }

    /**
     * Return new dimensions if the image provided needs scaling down to make it fit the supplied
     * thumbnail dimension, or null if rescaling isn't needed.
     */
    protected static Rectangle needsResizing (Integer thumbSize, BufferedImage image)
    {
        if (thumbSize == null) {
            return null;
        }
        Rectangle target = thumbnailDimensions(thumbSize);
        if (!target.covers(new Rectangle(image))) {
            return target;
        }
        return null;
    }

    /**
     * Return the dimensions of a given thumbnail size.
     */
    public static Rectangle thumbnailDimensions (int thumbSize)
    {
        return new Rectangle(MediaDescSize.getWidth(thumbSize), MediaDescSize.getHeight(thumbSize));
    }

    /**
     * Computes and fills in the constraints on the supplied image, scaling thumbnails as
     * necessary, and publishes the image data to the media store.
     *
     * @param thumbSize the size of the thumbnail to generate, or null to omit the thumbnail.
     *
     * @return a MediaInfo object filled in with the published image info.
     */
    public static PublishingResult publishImage (
        Integer thumbSize, UploadFile uploadFile, byte thumbMime, String thumbFormat)
        throws IOException
    {
        // convert the uploaded file data into an image object
        final BufferedImage original;
        try {
            original = ImageIO.read(uploadFile.getInputStream());
            if (original == null) {
                throw new Exception("Unknown format.");
            }
        } catch (Exception e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }

        // if we're uploading a thumbnail image...
        Rectangle target = needsResizing(thumbSize, original);
        if (target != null) {
            return publishReducedSize(thumbSize, thumbMime, thumbFormat, original, target);
        }

        return publishOriginalSize(thumbSize, uploadFile, original);
    }

    /**
     * Shortcut for uploading a thumbnail sized image, resizing if necessary.
     */
    public static PublishingResult publishThumbnail (UploadFile uploadFile)
        throws IOException
    {
        return publishImage(MediaDescSize.THUMBNAIL_SIZE, uploadFile,
            uploadFile.getMimeType(), THUMBNAIL_IMAGE_FORMAT);
    }

    /**
     * Publish an image at it's original size and return a publishing result.
     */
    protected static PublishingResult publishOriginalSize (
        Integer thumbSize, UploadFile uploadFile, final BufferedImage original)
        throws IOException
    {
        // if we haven't yet published our image, do so
        // (this happens when we didn't need to scale the image to produce a thumbnail)
        Digester digester = new Digester();
        digester.digest(uploadFile.getInputStream());
        publishStreamAsHash(digester.getInputStream(), digester.hexHash(),
            uploadFile.getMimeType());

        Rectangle originalSize = new Rectangle(original);
        return new PublishingResult(originalSize, originalSize, thumbSize,
            uploadFile.getMimeType(), digester);
    }

    /**
     * Reduce the size of an image to the specified target size and publish it.
     */
    protected static PublishingResult publishReducedSize (
        Integer thumbSize, byte thumbMime, String thumbFormat, final BufferedImage original,
        Rectangle target)
        throws IOException
    {
        final BufferedImage thumbnail = resizeImage(thumbFormat, original, target);

        Digester digester = new Digester();
        ImageIO.write(thumbnail, thumbFormat, new MemoryCacheImageOutputStream(
            digester.getOutputStream()));
        digester.close();

        // publish the thumbnail
        publishStreamAsHash(digester.getInputStream(), digester.hexHash(), thumbMime);

        return new PublishingResult(new Rectangle(original), target, thumbSize, thumbMime,
            digester);
    }

    /**
     * Resize a given image to a new set of dimensions. The buffered image will use a sampling
     * model appropriate to the given format so that it can be successfully encoded.
     */
    private static BufferedImage resizeImage (
        String format, final BufferedImage image, final Rectangle target)
        throws IOException
    {
        final BufferedImage timage;
        final float tratio = target.width / (float)target.height;
        final float iratio = image.getWidth() / (float)image.getHeight();
        final float scale = iratio > tratio ? target.width / (float)image.getWidth()
            : target.height / (float)image.getHeight();
        int newWidth = Math.max(1, Math.round(scale * image.getWidth()));
        int newHeight = Math.max(1, Math.round(scale * image.getHeight()));

        // generate the scaled image
        timage = new BufferedImage(newWidth, newHeight, samplingModelForFormat(format));
        final Graphics2D gfx = timage.createGraphics();
        try {
            Image scaledImg = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            gfx.drawImage(scaledImg, 0, 0, null);
        } finally {
            gfx.dispose();
        }
        return timage;
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
            throw new IOException("image sampling model not known for format: " + informalName);
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
        "tiff", BufferedImage.TYPE_INT_ARGB);

    protected static final byte THUMBNAIL_MIME_TYPE = MediaMimeTypes.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";

    // Effectively 'never' expire date.
    protected static final Map<String, String> EXPIRES_2038 =
        ImmutableMap.of("Expires", "Sun, 17 Jan 2038 19:14:07 GMT");
}
