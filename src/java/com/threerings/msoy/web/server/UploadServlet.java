//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.s3.AWSAuthConnection;
import com.threerings.s3.S3FileObject;
import com.threerings.s3.S3Exception;

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
        if (length > MAX_UPLOAD_SIZE) {
            rsp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        // TODO: create a custom file item factory that just puts items in the
        // right place from the start and computes the SHA hash on the way
        ServletFileUpload upload =
            new ServletFileUpload(new DiskFileItemFactory());
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
                             ", size=" + item.getSize() +
                             ", id=" + item.getFieldName() + "].");
                    mediaId = item.getFieldName();
                    info = handleFileItem(item, mediaId);
                }
            }

        } catch (FileUploadException e) {
            log.info("File upload choked: " + e + ".");
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
            String thash = (info[1] == null) ? null : info[1].hash;
            int tmime = (info[1] == null) ? 0 : info[1].mimeType;
            String script = "parent.setHash('" + mediaId + "', '" +
                info[0].hash + "', " + info[0].mimeType + ", " + info[0].constraint + ", '" +
                thash + "', " + tmime + ")";
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
    protected MediaInfo[] handleFileItem (FileItem item, String mediaId)
        throws IOException
    {
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
        MediaInfo info = new MediaInfo(), tinfo = null;
        info.hash = StringUtil.hexlate(digest.digest());

        // TODO: this will have to change. We cannot depend on the user supplying us with a valid
        // content type, not because of malice, but because it's quite common to have a file type
        // that your own computer doesn't understand but which you can play on the web.

        // look up the mime type
        info.mimeType = MediaDesc.stringToMimeType(item.getContentType());
        if (info.mimeType == -1) {
            // if that failed, try inferring the type from the path
            info.mimeType = MediaDesc.suffixToMimeType(item.getName());
        }
        if (info.mimeType == -1) {
            log.warning("Received upload of unknown mime type [type=" + item.getContentType() +
                        ", name=" + item.getName() + "].");
            return null;
        }

        // if this is an image, determine its constraints and generate a thumbnail
        if (MediaDesc.isImage(info.mimeType)) {
            tinfo = processImage(output, digest, info);
        }

        // if the user is uploading a thumbnail image, we want to use the scaled version and
        // abandon their original
        if (tinfo != null && mediaId.equals(Item.THUMB_ID)) {
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
                return null;
            }

            // publish this file to S3 if desired
            publishFile(target, name, info);
        }

        return new MediaInfo[] { info, tinfo };
    }

    /**
     * Publishes a file to S3, if enabled.
     */
    protected void publishFile (File target, String name, MediaInfo info)
        throws IOException
    {
        if (ServerConfig.mediaS3Enable) {
            try {
                AWSAuthConnection conn = new AWSAuthConnection(
                    ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);
                S3FileObject uploadTarget = new S3FileObject(
                    name, target, MediaDesc.mimeTypeToString(info.mimeType));
                conn.putObject(ServerConfig.mediaS3Bucket, uploadTarget);
            } catch (S3Exception e) {
                log.warning("S3 upload failed [code=" + e.getClass().getName() +
                            ", requestId=" + e.getRequestId() + ", hostId=" + e.getHostId() +
                            ", message=" + e.getMessage() + "].");
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
        info.constraint =  MediaDesc.computeConstraint(
            Item.PREVIEW_WIDTH, Item.PREVIEW_HEIGHT, image.getWidth(), image.getHeight());

        // generate a thumbnail for this image
        MediaInfo tinfo = new MediaInfo();
        tinfo.constraint = MediaDesc.computeConstraint(
            Item.THUMBNAIL_WIDTH, Item.THUMBNAIL_HEIGHT, image.getWidth(), image.getHeight());
        if (tinfo.constraint == MediaDesc.NOT_CONSTRAINED) {
            // if it's really small, we can use the original as the thumbnail
            tinfo.hash = info.hash;
            tinfo.mimeType = info.mimeType;

        } else {
            // scale the image to thumbnail size
            float scale = (tinfo.constraint == MediaDesc.HORIZONTALLY_CONSTRAINED) ?
                (float)Item.THUMBNAIL_WIDTH / image.getWidth()  : 
                (float)Item.THUMBNAIL_HEIGHT / image.getHeight();
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
    }

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int MAX_UPLOAD_SIZE = 10 * 1024 * 1024;

    /** Le chunk! */
    protected static final int UPLOAD_BUFFER_SIZE = 4096;

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
}
