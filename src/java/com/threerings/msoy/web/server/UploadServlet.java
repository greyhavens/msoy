//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import java.security.DigestOutputStream;
import java.security.MessageDigest;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;
import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3FileObject;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3ServerException;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles the uploading of digital media for later use by a digital item.
 */
public class UploadServlet extends HttpServlet
{
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        int length = req.getContentLength();
        if (length <= 0) {
            rsp.sendError(HttpServletResponse.SC_LENGTH_REQUIRED);
            return;
        }
        // check against our largest size immediately
        if (length > LARGE_MEDIA_MAX_SIZE) {
            rsp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        // TODO: create a custom file item factory that just puts items in the right place from the
        // start and computes the SHA hash on the way
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        MediaInfo[] info = null;
        String mediaId = null;
        try {
            for (Object obj : upload.parseRequest(req)) {
                FileItem item = (FileItem)obj;
                if (item.isFormField()) {
                    // currently, we don't care about these

                } else {
                    // TODO: check that this is a supported content type
                    log.info("Receiving file [type: " + item.getContentType() +
                             ", size=" + item.getSize() + ", id=" + item.getFieldName() + "].");
                    mediaId = item.getFieldName();
                    info = handleFileItem(item, mediaId, length);
                }
            }

        } catch (ServletFileUpload.SizeLimitExceededException slee) {
            log.info("File upload too big: " + slee + ".");
            // TODO: use slee.getActualSize(), slee.getPermittedSize()?
            // getActualSize is probably a little off, as we build the exception
            // with the total request length
            rsp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;

        } catch (FileUploadException fue) {
            log.info("File upload choked: " + fue + ".");
            // TODO: send JavaScript that communicates a friendly error
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // if we parsed no info, handleFileItem will have logged an error or the user didn't send
        // anything; TODO: send JavaScript that communicates a friendly error
        if (info == null || mediaId == null) {
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // write out the magical incantations that are needed to cause our magical little frame to
        // communicate the newly assigned mediaHash to the ItemEditor widget
        PrintStream out = new PrintStream(rsp.getOutputStream());
        try {
            out.println("<html>");
            out.println("<head></head>");
            String thash = (info[1] == null) ? "" : info[1].hash;
            int tmime = (info[1] == null) ? 0 : info[1].mimeType;
            int tconstraint = (info[1] == null) ? 0 : info[1].constraint;
            String script = "parent.setHash('" + mediaId + "', " +
                "'" + info[0].hash + "', " + info[0].mimeType + ", " + info[0].constraint + ", " +
                info[0].width + ", " + info[0].height + ", " +
                "'" + thash + "', " + tmime + ", " + tconstraint + ")";
            out.println("<body onLoad=\"" + script + "\"></body>");
            out.println("</html>");
        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Computes and returns the SHA hash and mime type of the supplied item and puts it in the
     * proper place in the media upload directory.
     */
    protected MediaInfo[] handleFileItem (FileItem item, String mediaId, int uploadLength)
        throws IOException, FileUploadException
    {
        MediaInfo info = new MediaInfo(), tinfo = null;

        // look up the mime type
        info.mimeType = MediaDesc.stringToMimeType(item.getContentType());
        // if that failed, try inferring the type from the path
        if (info.mimeType == -1) {
            info.mimeType = MediaDesc.suffixToMimeType(item.getName());
        }
        if (info.mimeType != -1) {
            // if we now know the mimetype, check the length
            validateFileLength(info.mimeType, uploadLength);
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create message digest.", e);
            return null;
        }

        // first write the file to disk under a temporary name and compute its digest in the
        // process
        InputStream in = item.getInputStream();
        File output = File.createTempFile("upload", ".tmp", ServerConfig.mediaDir);
        FileOutputStream out = null;
        try {
            byte[] buffer = new byte[UPLOAD_BUFFER_SIZE];
            out = new FileOutputStream(output);
            long length = item.getSize();
            while (length > 0) {
                int read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                length -= read;
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }

        } finally {
            StreamUtil.close(out);
            StreamUtil.close(in);
        }

        // compute the file hash
        info.hash = StringUtil.hexlate(digest.digest());

        // if we could not discern from the file path, try determining the mime type
        // from the file data itself.
        if (info.mimeType == -1) {
            FileInputStream idStream = new FileInputStream(output);
            byte[] firstBytes = new byte[_mimeMagic.getMinArrayLength()];
            String mimeString = null;

            // Read identifying bytes from the uploaded file
            if (idStream.read(firstBytes, 0, firstBytes.length) >= firstBytes.length) {
                // Sufficient data was read, attempt magic identification
                mimeString = _mimeMagic.identify(firstBytes, item.getName(), null);

                // Map to mime MediaDesc Mime byte
                info.mimeType = MediaDesc.stringToMimeType(mimeString);
                
                if (mimeString != null) {
                    // XXX debugging; want to know the effectiveness, any false hits,
                    // and what types of files are being uploaded -- landonf (March 5, 2007)
                    log.warning("Magically determined unknown mime type [type=" + mimeString +
                                ", name=" + item.getName() + "].");                                  
                }
            }

            // finally, check the file size now that we know mimetype,
            // or freak out if we still don't know the mimetype.
            if (info.mimeType != -1) {
                // now we can validate the file size
                try {
                    validateFileLength(info.mimeType, uploadLength);

                } catch (FileUploadException fue) {
                    output.delete();
                    throw fue;
                }

            } else {
                log.warning("Received upload of unknown mime type [type=" + item.getContentType() +
                    ", name=" + item.getName() + "].");
                output.delete();
                return null;
            }
        }

        // if this is an image, determine its constraints and generate a thumbnail
        if (MediaDesc.isImage(info.mimeType)) {
            tinfo = processImage(output, digest, info);
        }

        // if the user is uploading a thumbnail image, we want to use the scaled version and
        // abandon their original
        if (tinfo != null && !tinfo.hash.equals(info.hash) && mediaId.equals(Item.THUMB_MEDIA)) {
            info = tinfo;
            tinfo = null;
            if (!output.delete()) {
                log.warning("Unable to delete unscaled thumnail image '" + output + "'.");
            }

        } else {
            // now name it using the hash value and the suffix
            String name = info.hash + MediaDesc.mimeTypeToSuffix(info.mimeType);
            File target = getMediaFilePath(name);
            if (!output.renameTo(target)) {
                log.warning("Unable to rename uploaded file [temp=" + output +
                            ", perm=" + target + "].");
                output.delete();
                return null;
            }

            // publish this file to S3 if desired
            publishFile(target, name, info);
        }

        return new MediaInfo[] { info, tinfo };
    }

    /**
     * Validate that the upload size is acceptable for the specified mimetype.
     */
    protected void validateFileLength (byte mimeType, int length)
        throws FileUploadException
    {
        int limit;
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
     * Publishes a file to S3, if enabled.
     */
    protected void publishFile (File target, String name, MediaInfo info)
        throws IOException
    {
        if (ServerConfig.mediaS3Enable) {
            try {
                S3Connection conn = new S3Connection(
                    ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);

                S3FileObject uploadTarget = new S3FileObject(
                    name, target, MediaDesc.mimeTypeToString(info.mimeType));

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
     * Computes and fills in the constraints on the supplied image and generates a thumbnail
     * representation.
     *
     * @return a MediaInfo record for the generated thumbnail.
     */
    protected MediaInfo processImage (File output, MessageDigest digest, MediaInfo info)
        throws IOException
    {
        BufferedImage image = ImageIO.read(output);

        // determine whether this image is width or height constrained
        info.width = image.getWidth();
        info.height = image.getHeight();
        info.constraint =  MediaDesc.computeConstraint(
            MediaDesc.PREVIEW_SIZE, info.width, info.height);

        // generate a thumbnail for this image
        MediaInfo tinfo = new MediaInfo();
        tinfo.constraint = MediaDesc.computeConstraint(
            MediaDesc.THUMBNAIL_SIZE, image.getWidth(), image.getHeight());
        if (tinfo.constraint == MediaDesc.NOT_CONSTRAINED ||
            tinfo.constraint == MediaDesc.HALF_HORIZONTALLY_CONSTRAINED ||
            tinfo.constraint == MediaDesc.HALF_VERTICALLY_CONSTRAINED) {
            // if it's really small, we can use the original as the thumbnail
            tinfo.hash = info.hash;
            tinfo.mimeType = info.mimeType;

        } else {
            // scale the image to thumbnail size
            float scale = (tinfo.constraint == MediaDesc.HORIZONTALLY_CONSTRAINED) ?
                (float)MediaDesc.THUMBNAIL_WIDTH / image.getWidth()  : 
                (float)MediaDesc.THUMBNAIL_HEIGHT / image.getHeight();
            int twidth =  Math.round(scale * image.getWidth());
            int theight =  Math.round(scale * image.getHeight());
            BufferedImage timage = new BufferedImage(twidth, theight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gfx = timage.createGraphics();
            try {
//                 gfx.drawImage(image.getScaledInstance(twidth, theight, BufferedImage.SCALE_FAST),
//                               0, 0, null);
                gfx.drawImage(image, 0, 0, twidth, theight, null);
            } finally {
                gfx.dispose();
            }

            // now encode it into the target image format and compute its hash along the way
            digest.reset();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DigestOutputStream digout = new DigestOutputStream(bout, digest);
            ImageIO.write(timage, THUMBNAIL_IMAGE_FORMAT,
                          new MemoryCacheImageOutputStream(digout));
            tinfo.hash = StringUtil.hexlate(digest.digest());
            tinfo.mimeType = THUMBNAIL_MIME_TYPE;

            // finally write the bytes to the file system
            String tname = tinfo.hash + MediaDesc.mimeTypeToSuffix(tinfo.mimeType);
            File target = getMediaFilePath(tname);
            FileOutputStream fout = new FileOutputStream(target);
            fout.write(bout.toByteArray());
            fout.close();

            log.info("Generated thumbnail [file=" + tname + ", width=" + twidth +
                     ", height=" + theight + "].");

            // publish this file to S3 if desired
            publishFile(target, tname, tinfo);
        }

        return tinfo;
    }

    protected File getMediaFilePath (String name)
    {
        // TODO: turn XXXXXXX... into XX/XX/XXXX... to avoid freaking out the file system with the
        // amazing four hundred billion files
        return new File(ServerConfig.mediaDir, name);
    }

    protected static class MediaInfo
    {
        public String hash;
        public byte mimeType;
        public byte constraint;
        public int width, height;
    }

    /** A magic mime type identifier. */
    protected final MimeTypeIdentifier _mimeMagic = new MagicMimeTypeIdentifier();

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int MEGABYTE = 1024 * 1024;
    protected static final int SMALL_MEDIA_MAX_SIZE = 4 * MEGABYTE;
    protected static final int LARGE_MEDIA_MAX_SIZE = 100 * MEGABYTE;

    /** Le chunk! */
    protected static final int UPLOAD_BUFFER_SIZE = 4096;

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
}
