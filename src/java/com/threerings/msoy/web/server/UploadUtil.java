//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Collections;

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
            this.hash = other.hash;
            this.mimeType = other.mimeType;
            this.constraint = constraint;
            this.width = other.width;
            this.height = other.height;
        }
    }

    /**
     * Publishes an InputStream to the default media store location, using the hash and the
     * mimeType of the stream.
     */
    public static void publishStream (InputStream input, String hash, byte mimeType)
        throws IOException
    {
        // name it using the hash value and the suffix
        String name = hash + MediaDesc.mimeTypeToSuffix(mimeType);
        publishStream(input, null, name, MediaDesc.mimeTypeToString(mimeType), EXPIRES_2038);
    }

    /**
     * Publishes an InputStream to the default media store location, using name of the stream
     * (usually its hash) and a supplied mime type.
     */
    public static void publishStream (InputStream input, String name, String mimeType)
        throws IOException
    {
        publishStream(input, null, name, mimeType);
    }

    /**
     * Publishes an InputStream to the media store at the specified subdirectory location,
     * using name of the stream (usually its hash) and a supplied mime type.
     * Currently this is to the filesystem first, and then s3 if enabled.
     */
    public static void publishStream (
        InputStream input, String subdirectory, String name, String mimeType)
        throws IOException
    {
        publishStream(input, subdirectory, name, mimeType, null);
    }

    /**
     * Publishes an InputStream to the media store at the specified subdirectory location,
     * using name of the stream (usually its hash) and a supplied mime type.
     * Currently this is to the filesystem first, and then s3 if enabled. A map of headers
     * to be added to the s3 object may be supplied.
     */
    public static void publishStream (InputStream input, String subdirectory, String name,
                                      String mimeType, Map<String,String> headers)
        throws IOException
    {
        // copy the uploaded file data to the local file system media store. eventually we will
        // only be keeping a local file on developer's machines

        File location = ServerConfig.mediaDir; // default media directory

        // if a subdirectory was specified, set that as the new location, and make sure it exists
        if (subdirectory != null) {
            location = new File(ServerConfig.mediaDir, subdirectory);
            if (! location.exists()) {
                location.mkdir();
            }
        }

        // our new file location
        File target = new File(location, name);
        IOUtils.copy(input, new FileOutputStream(target));

        // publish to s3 if enabled
        // TODO: check if this hash is already in the media store and skip the upload if found
        if (ServerConfig.mediaS3Enable) {
            try {
                S3Connection conn = new S3Connection(
                    ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);
                S3FileObject uploadTarget = new S3FileObject(name, target, mimeType);

                if (headers == null ) {
                    headers = Collections.emptyMap();
                }

                conn.putObject(ServerConfig.mediaS3Bucket, uploadTarget,
                               AccessControlList.StandardPolicy.PUBLIC_READ, headers);

                log.info("Uploaded media to S3 [bucket=" + ServerConfig.mediaS3Bucket +
                         ", name=" + name + "].");

            } catch (S3ServerException e) {
                // S3 Server-side Exception
                log.warning("S3 upload failed [code=" + e.getClass().getName() +
                            ", requestId=" + e.getRequestId() + ", hostId=" + e.getHostId() +
                            ", message=" + e.getMessage() + "].");

            } catch (S3Exception e) {
                // S3 Client-side Exception
                log.warning("S3 upload failed: " + e);
            }
        }
    }

    /**
     * Publishes a screenshot file with a meaningful filename.
     */
    public static void publishSnapshot (SnapshotUploadFile uploadFile)
        throws IOException
    {
        // the snapshot gets uploaded to /snapshot/<
        byte mimeType = uploadFile.getMimeType();
        String sceneId = String.valueOf(uploadFile.getSceneId());
        String mimeSuffix = MediaDesc.mimeTypeToSuffix(mimeType);
        publishStream(uploadFile.getInputStream(), SNAPSHOT_DIRECTORY,
            sceneId + mimeSuffix, MediaDesc.mimeTypeToString(uploadFile.getMimeType()));
        publishImage(MediaDesc.SNAPSHOT_THUMB_SIZE, uploadFile, mimeType, "jpg",
            SNAPSHOT_DIRECTORY, sceneId + "_t" + mimeSuffix);
    }

    /**
     * Publishes an UploadFile to the media store.
     */
    public static void publishUploadFile (UploadFile uploadFile)
        throws IOException
    {
        publishStream(uploadFile.getInputStream(), uploadFile.getHash(), uploadFile.getMimeType());
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
        return publishImage(size, uploadFile, THUMBNAIL_MIME_TYPE, THUMBNAIL_IMAGE_FORMAT,
            null, null);
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
        int size, UploadFile uploadFile, byte thumbMime, String thumbFormat,
        String dir, String name)
        throws IOException
    {
        // convert the uploaded file data into an image object
        BufferedImage image = ImageIO.read(uploadFile.getInputStream());
        if (image == null) {
            throw new IOException("Invalid image data. Unable to complete upload.");
        }

        String hash = uploadFile.getHash();
        byte mimeType = uploadFile.getMimeType();
        int width = image.getWidth();
        int height = image.getHeight();

        // if we're uploading a thumbnail image...
        boolean published = false;
        if (size != MediaDesc.PREVIEW_SIZE) {
            int targetWidth = MediaDesc.DIMENSIONS[size * 2];
            int targetHeight = MediaDesc.DIMENSIONS[size * 2 + 1];
            // ...and we may need to scale our image
            if (width > targetWidth || height > targetHeight) {
                // determine the size of our to be scaled image
                float tratio = targetWidth / (float)targetHeight;
                float iratio = image.getWidth() / (float)image.getHeight();
                float scale = (iratio > tratio) ?
                    targetWidth / (float)image.getWidth() :
                    targetHeight / (float)image.getHeight();
                width = Math.max(1, Math.round(scale * image.getWidth()));
                height = Math.max(1, Math.round(scale * image.getHeight()));

                // generate the scaled image
                BufferedImage timage =
                    new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gfx = timage.createGraphics();
                try {
                    gfx.drawImage(image, 0, 0, width, height, null);
                } finally {
                    gfx.dispose();
                }

                // encode it into the target image format and compute its hash along the way
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException nsa) {
                    throw new RuntimeException(nsa.getMessage());
                }
                mimeType = thumbMime;

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DigestOutputStream digout = new DigestOutputStream(bout, digest);
                ImageIO.write(timage, thumbFormat, new MemoryCacheImageOutputStream(digout));

                // update our hash and thumbnail
                hash = StringUtil.hexlate(digest.digest());

                // publish the thumbnail
                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                if (name == null) {
                    publishStream(bin, hash, mimeType);
                } else {
                    publishStream(bin, dir, name, MediaDesc.mimeTypeToString(mimeType));
                }
                bout.close();
                published = true;
            }
        }

        // if we haven't yet published our image, do so
        if (!published) {
            publishUploadFile(uploadFile);
        }

        // finally compute our constraint and return a media info
        byte constraint = MediaDesc.computeConstraint(size, width, height);
        return new MediaInfo(hash, mimeType, constraint, width, height);
    }

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
    protected static final String SNAPSHOT_DIRECTORY = "snapshot";

    // Effectively 'never' expire date.
    protected static final Map<String,String> EXPIRES_2038 =
        ImmutableMap.of("Expires", "Sun, 17 Jan 2038 19:14:07 GMT");
}
