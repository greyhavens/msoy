//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.server.UploadUtil.FullMediaInfo;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

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
    protected void handleFileItems (FileItem item, FileItem[] allItems, int uploadLength,
                                    HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        FullMediaInfo fullInfo = null;
        
        // wrap the FileItem in an UploadFile for publishing
        UploadFile uploadFile = new FileItemUploadFile(item);
        
        // attempt to extract the mediaId
        String mediaId = item.getFieldName();
        if (mediaId == null) {
            throw new FileUploadException("Failed to extract mediaId from upload request.");
        }

        // TODO: check that the user is logged in. This will require the various item editors to
        // pass a WebIdent down here.

        // TODO: check that this is a supported content type
        log.info("Received file: [type: " + item.getContentType() + ", size="
            + item.getSize() + ", id=" + item.getFieldName() + "].");

        // check the file size now that we know mimetype,
        // or freak out if we still don't know the mimetype.
        if (uploadFile.getMimeType() != MediaDesc.INVALID_MIME_TYPE) {
            // now we can validate the file size
            validateFileLength(uploadFile.getMimeType(), uploadLength);

        } else {
            throw new FileUploadException("Received upload of unknown mime type [type=" +
                item.getContentType() + ", name=" + item.getName() + "].");
        }

        // if this is an image, determine its constraints, generate a thumbnail, and publish
        // the data into the media store
        if (MediaDesc.isImage(uploadFile.getMimeType())) {
            fullInfo = UploadUtil.publishImage(uploadFile, mediaId);

            // treat all other file types in the same manner
        } else {
            MediaInfo info = new MediaInfo(uploadFile.getHash(), uploadFile.getMimeType());

            // publish the file
            UploadUtil.publishUploadFile(uploadFile);

            // the full media info is just the item and a blank thumbnail
            fullInfo = new FullMediaInfo(info, new MediaInfo(), mediaId);
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

        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            limit = MEDIUM_MEDIA_MAX_SIZE;
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

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int SMALL_MEDIA_MAX_SIZE = 5 * MEGABYTE;
    protected static final int MEDIUM_MEDIA_MAX_SIZE = 10 * MEGABYTE;
    protected static final int LARGE_MEDIA_MAX_SIZE = 100 * MEGABYTE;
}
