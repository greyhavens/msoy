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

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.samskivert.util.StringUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.SnapshotMediaDesc;
import com.threerings.msoy.server.ServerConfig;
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
     * A class to hold an item and its generated thumbnail, if needed.
     */
    public static class FullMediaInfo
    {
        public final MediaInfo item;
        public final MediaInfo thumb;
        public final String mediaId;

        public FullMediaInfo (MediaInfo item, MediaInfo thumb, String mediaId)
        {
            this.item = item;
            this.thumb = thumb;
            this.mediaId = mediaId;
        }
    }

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
     * Publishes an InputStream to the media store using the hash and mimeType of the stream.
     */
    public static void publishStream (InputStream input, String hash, byte mimeType)
        throws IOException
    {
        // name it using the hash value and the suffix
        String name = hash + MediaDesc.mimeTypeToSuffix(mimeType);
        publishStream (input, name, MediaDesc.mimeTypeToString(mimeType));
    }

    /**
     * Publishes an InputStream to the media store using the hash of the stream and a supplied
     * "file" extension.
     * Currently this is to the filesystem first, and then s3 if enabled.
     */
    public static void publishStream (InputStream input, String name, String extension)
        throws IOException
    {
        File target = new File(ServerConfig.mediaDir, name);

        // copy the uploaded file data to the local file system media store. eventually we will
        // only be keeping a local file on developer's machines
        IOUtils.copy(input, new FileOutputStream(target));

        // publish to s3 if enabled
        // TODO: check if this hash is already in the media store and skip the upload if found
        if (ServerConfig.mediaS3Enable) {
            try {
                S3Connection conn = new S3Connection(
                    ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);
                S3FileObject uploadTarget = new S3FileObject(name, target, extension);
                conn.putObject(ServerConfig.mediaS3Bucket, uploadTarget,
                               AccessControlList.StandardPolicy.PUBLIC_READ);
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
        String name =
            SnapshotMediaDesc.sceneToName(uploadFile.getSceneId()) +
            SnapshotMediaDesc.mimeTypeToSuffix(uploadFile.getMimeType());
        publishStream(uploadFile.getInputStream(),
                      name, SnapshotMediaDesc.mimeTypeToString(uploadFile.getMimeType()));
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
     * Computes and fills in the constraints on the supplied image, generates a thumbnail
     * representation, and publishes the image data to the media store.
     *
     * @return a FullMediaInfo object filled in with the item and thumbnail.
     */
    public static FullMediaInfo publishImage (UploadFile uploadFile, String mediaId)
        throws IOException
    {
        // convert the uploaded file data into an image object
        BufferedImage image = ImageIO.read(uploadFile.getInputStream());
        if (image == null) {
            throw new IOException("Invalid image data. Unable to complete upload.");
        }

        // create the media info object for this image
        byte constraint = MediaDesc.computeConstraint(
            MediaDesc.PREVIEW_SIZE, image.getWidth(), image.getHeight());
        MediaInfo info = new MediaInfo(uploadFile.getHash(), uploadFile.getMimeType(),
                                       constraint, image.getWidth(), image.getHeight());

        // generate a thumbnail for this image
        byte tconstraint = MediaDesc.computeConstraint(
            MediaDesc.THUMBNAIL_SIZE, image.getWidth(), image.getHeight());
        MediaInfo tinfo = null;

        if (tconstraint == MediaDesc.NOT_CONSTRAINED ||
            tconstraint == MediaDesc.HALF_HORIZONTALLY_CONSTRAINED ||
            tconstraint == MediaDesc.HALF_VERTICALLY_CONSTRAINED) {
            // if it's really small, we can use the original as the thumbnail, but preserve
            // tconstraint because that might be different because we compute thumbnail image
            // constraints using half-thumbnail sizes; whee!
            tinfo = new MediaInfo(info, tconstraint);

        } else {
            // scale the image to thumbnail size
            float scale = (tconstraint == MediaDesc.HORIZONTALLY_CONSTRAINED) ?
                (float)MediaDesc.THUMBNAIL_WIDTH / image.getWidth() :
                (float)MediaDesc.THUMBNAIL_HEIGHT / image.getHeight();
            int twidth = Math.round(scale * image.getWidth());
            int theight = Math.round(scale * image.getHeight());
            BufferedImage timage = new BufferedImage(twidth, theight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gfx = timage.createGraphics();
            try {
                // gfx.drawImage(image.getScaledInstance(twidth, theight, BufferedImage.SCALE_FAST),
                //     0, 0, null);
                gfx.drawImage(image, 0, 0, twidth, theight, null);
            } finally {
                gfx.dispose();
            }

            // now encode it into the target image format and compute its hash along the way
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsa) {
                throw new RuntimeException(nsa.getMessage());
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DigestOutputStream digout = new DigestOutputStream(bout, digest);
            ImageIO.write(timage, THUMBNAIL_IMAGE_FORMAT,
                new MemoryCacheImageOutputStream(digout));
            String thash = StringUtil.hexlate(digest.digest());
            tinfo = new MediaInfo(thash, THUMBNAIL_MIME_TYPE, tconstraint, twidth, theight);

            // publish the thumbnail
            publishStream(new ByteArrayInputStream(bout.toByteArray()), tinfo.hash, tinfo.mimeType);
            bout.close();
        }

        // if the user is uploading a thumbnail image, we want to use the scaled version and
        // abandon their original uploaded file data
        if (!tinfo.hash.equals(info.hash) && mediaId.equals(Item.THUMB_MEDIA)) {
            // the generated thumbnail is the item, leave the thumbnail element blank
            return new FullMediaInfo(tinfo, new MediaInfo(), mediaId);

        } else {
            // publish the image
            publishUploadFile(uploadFile);
            return new FullMediaInfo(info, tinfo, mediaId);
        }
    }

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
}
