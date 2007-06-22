//
// $Id$

package com.threerings.msoy.web.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;
import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3FileObject;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3ServerException;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles the uploading of digital media for later use by a digital item.
 */
public class UploadServlet extends AbstractUploadServlet
{
    /**
     * Generates the FullMediaInfo object for this FileItem and publishes the data into the media
     * store and returns the results via Javascript to the GWT client.
     */
    @Override // from AbstractUploadServlet
    protected void handleUploadFile (UploadFile uploadFile, int uploadLength,
        HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        FullMediaInfo fullInfo = null;

        // attempt to extract the mediaId
        String mediaId = uploadFile.item.getFieldName();
        if (mediaId == null) {
            throw new FileUploadException("Failed to extract mediaId from upload request.");
        }

        // TODO: check that the user is logged in. This will require the various item editors to
        // pass a WebIdent down here.

        // TODO: check that this is a supported content type
        log.info("Received file: [type: " + uploadFile.item.getContentType() + ", size="
            + uploadFile.item.getSize() + ", id=" + uploadFile.item.getFieldName() + "].");

        // check the file size now that we know mimetype,
        // or freak out if we still don't know the mimetype.
        if (uploadFile.getMimeType() != MediaDesc.INVALID_MIME_TYPE) {
            // now we can validate the file size
            validateFileLength(uploadFile.getMimeType(), uploadLength);

        } else {
            throw new FileUploadException("Received upload of unknown mime type [type="
                + uploadFile.item.getContentType() + ", name=" + uploadFile.item.getName() + "].");
        }

        // if this is an image, determine its constraints, generate a thumbnail, and publish
        // the data into the media store
        if (MediaDesc.isImage(uploadFile.getMimeType())) {
            fullInfo = publishImage(uploadFile, mediaId);

            // treat all other file types in the same manner
        } else {
            MediaInfo info = new MediaInfo(uploadFile.generateHash(), uploadFile.getMimeType());

            // publish the file
            publishStream(uploadFile.item.getInputStream(), info);

            // the full media info is just the item and a blank thumbnail
            fullInfo = new FullMediaInfo(info, mediaId);
        }

        // display the full media info in GWT Javascript land
        displayResults(fullInfo, rsp);
    }

    @Override // from AbstractUploadServlet
    protected int getMaxUploadSize ()
    {
        return LARGE_MEDIA_MAX_SIZE;
    }

    /**
     * Display the FullMediaInfo in the GWT Javascript client.
     */
    protected void displayResults (FullMediaInfo fullInfo, HttpServletResponse rsp)
        throws IOException
    {
        // write out the magical incantations that are needed to cause our magical little frame to
        // communicate the newly assigned mediaHash to the ItemEditor widget
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html>");
            out.println("<head></head>");
            String script = "parent.setHash('" + fullInfo.mediaId + "', " + "'"
                + fullInfo.item.hash + "', " + fullInfo.item.mimeType + ", "
                + fullInfo.item.constraint + ", " + fullInfo.item.width
                + ", " + fullInfo.item.height + ", " + "'"
                + fullInfo.thumb.hash + "', " + fullInfo.thumb.mimeType
                + ", " + fullInfo.thumb.constraint + ")";
            out.println("<body onLoad=\"" + script + "\"></body>");
            out.println("</html>");

        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Validate that the upload size is acceptable for the specified mimetype.
     */
    protected void validateFileLength (byte mimeType, int length)
        throws FileUploadException
    {
        long limit;
        switch (mimeType) {
        case MediaDesc.AUDIO_MPEG:
        case MediaDesc.VIDEO_FLASH:
        case MediaDesc.VIDEO_MPEG:
        case MediaDesc.VIDEO_QUICKTIME:
        case MediaDesc.VIDEO_MSVIDEO:
            limit = LARGE_MEDIA_MAX_SIZE;
            break;

        default:
            limit = SMALL_MEDIA_MAX_SIZE;
            break;
        }

        // do the actual check
        if (length > limit) {
            throw new ServletFileUpload.SizeLimitExceededException(
                "File size is too big for specified mimeType", length, limit);
        }
    }

    /**
     * Publishes a file. Currently this is to the filesystem first, and then s3 if enabled.
     */
    protected void publishStream (InputStream input, MediaInfo info)
        throws IOException
    {
        // now name it using the hash value and the suffix
        String name = info.hash + MediaDesc.mimeTypeToSuffix(info.mimeType);
        File target = new File(ServerConfig.mediaDir, name);

        // copy the uploaded file data to the local file system media store. eventually we will 
        // only be keeping a local file on developer's machines
        IOUtils.copy(input, new FileOutputStream(target));

        // publish to s3 if enabled
        // TODO: check if this hash is already in the media store and skip the upload if found
        if (ServerConfig.mediaS3Enable) {
            try {
                S3Connection conn = new S3Connection(ServerConfig.mediaS3Id,
                    ServerConfig.mediaS3Key);

                S3FileObject uploadTarget = new S3FileObject(name, target,
                    MediaDesc.mimeTypeToString(info.mimeType));

                conn.putObject(ServerConfig.mediaS3Bucket, uploadTarget,
                    AccessControlList.StandardPolicy.PUBLIC_READ);

                log.info("Uploaded media to S3 [bucket=" + ServerConfig.mediaS3Bucket + ", name="
                    + name + "].");
            } catch (S3ServerException e) {
                // S3 Server-side Exception
                log.warning("S3 upload failed [code=" + e.getClass().getName() + ", requestId="
                    + e.getRequestId() + ", hostId=" + e.getHostId() + ", message="
                    + e.getMessage() + "].");
            } catch (S3Exception e) {
                // S3 Client-side Exception
                log.warning("S3 upload failed: " + e);
            }
        }
    }

    /**
     * Computes and fills in the constraints on the supplied image, generates a thumbnail
     * representation, and publishes the image data to the media store.
     *
     * @return a FullMediaInfo object filled in with the item and thumbnail.
     */
    protected FullMediaInfo publishImage (UploadFile uploadFile, String mediaId)
        throws IOException
    {
        // convert the uploaded file data into an image object
        BufferedImage image = ImageIO.read(uploadFile.item.getInputStream());
        if (image == null) {
            throw new IOException("Invalid image data. Unable to complete upload.");
        }

        // create the media info object for this image
        byte constraint = MediaDesc.computeConstraint(MediaDesc.PREVIEW_SIZE, image.getWidth(),
            image.getHeight());
        MediaInfo info = new MediaInfo(uploadFile.generateHash(), uploadFile.detectMimeType(),
            constraint, image.getWidth(), image.getHeight());

        // generate a thumbnail for this image
        byte tconstraint = MediaDesc.computeConstraint(MediaDesc.THUMBNAIL_SIZE,
            image.getWidth(), image.getHeight());
        MediaInfo tinfo = null;

        if (tconstraint == MediaDesc.NOT_CONSTRAINED
            || tconstraint == MediaDesc.HALF_HORIZONTALLY_CONSTRAINED
            || tconstraint == MediaDesc.HALF_VERTICALLY_CONSTRAINED) {
            // if it's really small, we can use the original as the thumbnail
            tinfo = (MediaInfo)info.clone();

        } else {
            // scale the image to thumbnail size
            float scale = (tconstraint == MediaDesc.HORIZONTALLY_CONSTRAINED)
                ? (float)MediaDesc.THUMBNAIL_WIDTH / image.getWidth()
                : (float)MediaDesc.THUMBNAIL_HEIGHT / image.getHeight();
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
            publishStream(new ByteArrayInputStream(bout.toByteArray()), tinfo);
            bout.close();
        }

        // if the user is uploading a thumbnail image, we want to use the scaled version and
        // abandon their original uploaded file data
        if (!tinfo.hash.equals(info.hash) && mediaId.equals(Item.THUMB_MEDIA)) {
            // the generated thumbnail is the item, leave the thumbnail element blank
            return new FullMediaInfo(tinfo, mediaId);

        } else {
            // publish the image
            publishStream(uploadFile.item.getInputStream(), info);

            return new FullMediaInfo(info, tinfo, mediaId);
        }
    }

    /**
     * A class to hold an item and its generated thumbnail, if needed.
     */
    protected static class FullMediaInfo
    {
        public final MediaInfo item;
        public final MediaInfo thumb;
        public final String mediaId;

        public FullMediaInfo (MediaInfo item, String mediaId)
        {
            // create a blank thumbnail
            this(item, new MediaInfo(), mediaId);
        }

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
    protected static class MediaInfo
        implements Cloneable
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

        @Override // from Object
        public Object clone ()
        {
            try {
                return super.clone();
            } catch (CloneNotSupportedException cnse) {
                throw new RuntimeException(cnse);
            }
        }

    }

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int SMALL_MEDIA_MAX_SIZE = 4 * MEGABYTE;
    protected static final int LARGE_MEDIA_MAX_SIZE = 100 * MEGABYTE;

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
}
