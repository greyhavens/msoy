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
import java.util.logging.Level;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
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
            log.info("File upload too big: [length= " + length + "].");
            uploadTooLarge(rsp);
            return;
        }

        // TODO: create a custom file item factory that just puts items in the right place from the
        // start and computes the SHA hash on the way
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(
            DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, ServerConfig.mediaDir));

        FileItem item = null;
        String mediaId = null;
        FullMediaInfo fullInfo = null;
        try {
           // attempt to extract the FileItem from the servlet request
           item = extractFileItem(req, upload);
           if (item == null) {
               log.warning("Failed to extract file from upload request. [req= " + req + "].");
               internalError(rsp);
               return;
           }

           // attempt to extract the mediaId
           mediaId = item.getFieldName();
           if (mediaId == null) {
               log.warning("Failed to extract mediaId from upload request. [req= " + req + "].");
               internalError(rsp);
               return;
           }

           // turn the FileItem into MediaInfo records
           fullInfo = handleFileItem(item, mediaId, length);
           if (fullInfo == null) {
               log.warning("Failed to extract MediaInfo from uploaded file. [file= " + item + "].");
               internalError(rsp);
               return;
           }

        } catch (ServletFileUpload.SizeLimitExceededException slee) {
            log.info("File upload too big: " + slee + ".");
            // TODO: use slee.getActualSize(), slee.getPermittedSize()?
            // getActualSize is probably a little off, as we build the exception
            // with the total request length
            uploadTooLarge(rsp);
            return;

        } catch (FileUploadException fue) {
            log.info("File upload choked: " + fue + ".");
            internalError(rsp);
            return;

        } catch (IOException ioe) {
            log.info("File upload choked during file i/o: " + ioe + ".");
            internalError(rsp);
            return;

        } finally {
            // delete the temporary upload file data.
            // item may be null if extractFileItem throws an exception
            if (item != null) {
                item.delete();
            }
        }

        // write out the magical incantations that are needed to cause our magical little frame to
        // communicate the newly assigned mediaHash to the ItemEditor widget
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html>");
            out.println("<head></head>");
            String script = "parent.setHash('" + mediaId + "', " +
                "'" + fullInfo.getItem().getHash() + "', " + fullInfo.getItem().getMimeType() +
                ", " + fullInfo.getItem().getConstraint() + ", " + fullInfo.getItem().getWidth() +
                ", " + fullInfo.getItem().getHeight() + ", " + "'" +
                fullInfo.getThumb().getHash() + "', " + fullInfo.getThumb().getMimeType() + ", " +
                fullInfo.getThumb().getConstraint() + ")";
            out.println("<body onLoad=\"" + script + "\"></body>");
            out.println("</html>");

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to setup OutputStream when finalizing upload.", ioe);
            internalError(rsp);
            return;

        } finally {
            StreamUtil.close(out);
        }
    }

    /*
     * Displays an internal error message to the user on the GWT client.
     */
    protected void internalError (HttpServletResponse rsp)
    {
        displayError(rsp, JavascriptError.UPLOAD_ERROR);
    }

    /*
     * Displays a message to the user on the GWT client that the upload was too large.
     */
    protected void uploadTooLarge (HttpServletResponse rsp)
    {
        displayError(rsp, JavascriptError.UPLOAD_TOO_LARGE);
    }

    /**
     * Calls the function from the supplied JavascriptError on the GWT side to display an
     * error message to the user.
     * @throws IOException
     */
    protected void displayError (HttpServletResponse rsp, JavascriptError error)
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html>");
            out.println("<head></head>");
            out.println("<body onLoad=\"parent." + error.function + "();\"></body>");
            out.println("</html>");

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to setup OutputStream when displaying error.", ioe);

        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Parse the upload request and return the first FileItem found. This will ignore
     * multiple file uploads if a multipart request is used. Returns null if no FileItem
     * could be found.
     * @throws FileUploadException
     */
    protected FileItem extractFileItem (HttpServletRequest req, ServletFileUpload upload)
        throws FileUploadException
    {
        for (Object obj : upload.parseRequest(req)) {
            FileItem item = (FileItem)obj;
            if (item.isFormField()) {
                // currently, we don't care about these

            } else {
                // TODO: check that this is a supported content type
                log.info("Receiving file [type: " + item.getContentType() +
                    ", size=" + item.getSize() + ", id=" + item.getFieldName() + "].");
                return item;
            }
        }
        return null;
    }

    /**
     * Generates the FullMediaInfo object for this FileItem and publishes the data into the media
     * store.
     */
    protected FullMediaInfo handleFileItem (FileItem item, String mediaId, int uploadLength)
        throws IOException, FileUploadException
    {
        UploadFile uploadFile = new UploadFile(item);

        // check the file size now that we know mimetype,
        // or freak out if we still don't know the mimetype.
        if (uploadFile.getMimeType() != MediaDesc.INVALID_MIME_TYPE) {
            // now we can validate the file size
            validateFileLength(uploadFile.getMimeType(), uploadLength);

        } else {
            log.warning("Received upload of unknown mime type [type=" +
                item.getContentType() + ", name=" + item.getName() + "].");
            return null;
        }

        // if this is an image, determine its constraints, generate a thumbnail, and publish
        // the data into the media store
        if (MediaDesc.isImage(uploadFile.getMimeType())) {
            return publishImage(uploadFile, mediaId.equals(Item.THUMB_MEDIA));

        // treat all other file types in the same manner
        } else {
            MediaInfo info = new MediaInfo(uploadFile.getHash(), uploadFile.getMimeType());

            // publish the file
            publishStream(item.getInputStream(), info);

            // the full media info is just the item and a blank thumbnail
            return new FullMediaInfo(info);
        }
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
     * Publishes a file. Currently this is to the filesystem first, and then s3 if enabled.
     */
    protected void publishStream (InputStream input, MediaInfo info)
        throws IOException
    {
        // now name it using the hash value and the suffix
        String name = info.getHash() + MediaDesc.mimeTypeToSuffix(info.getMimeType());
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

                S3FileObject uploadTarget = new S3FileObject(
                    name, target, MediaDesc.mimeTypeToString(info.getMimeType()));

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
     * Computes and fills in the constraints on the supplied image, generates a thumbnail
     * representation, and publishes the image data to the media store.
     *
     * @return a FullMediaInfo object filled in with the item and thumbnail.
     */
    protected FullMediaInfo publishImage (UploadFile uploadFile, boolean isThumbnail)
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
            // if it's really small, we can use the original as the thumbnail
            tinfo = (MediaInfo)info.clone();

        } else {
            // scale the image to thumbnail size
            float scale = (tconstraint == MediaDesc.HORIZONTALLY_CONSTRAINED) ?
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
        if (!tinfo.getHash().equals(info.getHash()) && isThumbnail) {
            // the generated thumbnail is the item, leave the thumbnail element blank
            return new FullMediaInfo(tinfo);

        } else {
            // publish the image
            publishStream(uploadFile.getInputStream(), info);

            return new FullMediaInfo(info, tinfo);
        }
    }

    /*
     * A class to hold an item and its generated thumbnail, if needed.
     */
    protected static class FullMediaInfo
    {

        public FullMediaInfo (MediaInfo item)
        {
            // create a blank thumbnail
            this(item, new MediaInfo());
        }

        public FullMediaInfo (MediaInfo item, MediaInfo thumb)
        {
            _item = item;
            _thumb = thumb;
        }

        public MediaInfo getItem ()
        {
            return _item;
        }

        public MediaInfo getThumb ()
        {
            return _thumb;
        }

        protected MediaInfo _item;
        protected MediaInfo _thumb;
    }

    /*
     * A data class used when generating the item javascript.
     */
    protected static class MediaInfo
        implements Cloneable
    {
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
            _hash = hash;
            _mimeType = mimeType;
            _constraint = constraint;
            _width = width;
            _height = height;
        }

        public byte getConstraint ()
        {
            return _constraint;
        }

        public String getHash ()
        {
            return _hash;
        }

        public int getHeight ()
        {
            return _height;
        }

        public byte getMimeType ()
        {
            return _mimeType;
        }

        public int getWidth ()
        {
            return _width;
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

        protected String _hash;
        protected byte _mimeType;
        protected byte _constraint;
        protected int _width;
        protected int _height;
    }

    /*
     * Stores the GWT javascript functions used for various error messages.
     */
    protected static enum JavascriptError
    {
        UPLOAD_ERROR ("uploadError"),
        UPLOAD_TOO_LARGE ("uploadTooLarge");

        public String function;

        JavascriptError (String function) {
            this.function = function;
        }
    }

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int MEGABYTE = 1024 * 1024;
    protected static final int SMALL_MEDIA_MAX_SIZE = 4 * MEGABYTE;
    protected static final int LARGE_MEDIA_MAX_SIZE = 100 * MEGABYTE;

    protected static final byte THUMBNAIL_MIME_TYPE = MediaDesc.IMAGE_PNG;
    protected static final String THUMBNAIL_IMAGE_FORMAT = "PNG";
}
