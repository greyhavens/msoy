//
// $Id$

package com.threerings.msoy.item.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.samskivert.io.StreamUtil;

// import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.web.server.AbstractUploadServlet;
import com.threerings.msoy.web.server.FileItemUploadFile;
import com.threerings.msoy.web.server.UploadFile;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;
import com.threerings.msoy.web.server.UploadUtil;

import static com.threerings.msoy.Log.log;

/**
 * Handles the uploading of digital media for later use by a digital item.
 */
public class ItemMediaUploadServlet extends AbstractUploadServlet
{
    /**
     * Generates the MediaInfo object for this FileItem and publishes the data into the media store
     * and returns the results via Javascript to the GWT client.
     */
    @Override // from AbstractUploadServlet
    protected void handleFileItems (UploadContext ctx)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // wrap the FileItem in an UploadFile for publishing
        UploadFile uploadFile = new FileItemUploadFile(ctx.file);
        String hash = uploadFile.getHash();

        checkBlacklist(hash);

        // attempt to extract the list of mediaIds. First will be the main media, followed
        // by optional thumb and/or furni mediaIds which indicate a thumb or furni mediaInfo
        // should be generated and returned along with the main media info.
        String fieldName = ctx.file.getFieldName();
        if (fieldName == null) {
            throw new FileUploadException("Failed to extract mediaId from upload request.");
        }
        List<String> mediaIds = Lists.newArrayList(fieldName.split(";"));

        // TODO: check that this is a supported content type
        log.info("Received file: [type: " + ctx.file.getContentType() +
                 ", size=" + ctx.file.getSize() + ", id=" + ctx.file.getFieldName() + "].");

        // check the file size now that we know mimetype,
        // or freak out if we still don't know the mimetype.
        byte mimeType = uploadFile.getMimeType();
        if (mimeType == MediaMimeTypes.INVALID_MIME_TYPE) {
            throw new FileUploadException("Received upload of unknown mime type [type=" +
                ctx.file.getContentType() + ", name=" + ctx.file.getName() + "].");
        }

        // now we can validate the file size
        validateFileLength(mimeType, ctx.uploadLength);

        // we'll return a mediaInfo for each mediaId supplied
        List<MediaInfo> mediaInfos = Lists.newArrayList();

        // if this is an image...
        if (MediaMimeTypes.isImage(mimeType)) {
            // ...determine its constraints, generate a thumbnail, and publish the data into the
            // media store
            String mainMediaId = mediaIds.get(0);
            mediaInfos.add(UploadUtil.publishImage(mainMediaId, uploadFile, false));

            for (int ii = 1; ii < mediaIds.size(); ii++) {
                String mediaId = mediaIds.get(ii);
                // if the client has requested scaled thumbnail or furni images to be generated
                // along with this image, do that as well. Furni in this case will be 320x200.
                if (Item.THUMB_MEDIA.equals(mediaId) && !Item.THUMB_MEDIA.equals(mainMediaId)) {
                    mediaInfos.add(UploadUtil.publishImage(Item.THUMB_MEDIA, uploadFile, true));
                } else if (Item.FURNI_MEDIA.equals(mediaId)
                    && !Item.FURNI_MEDIA.equals(mainMediaId)) {
                    mediaInfos.add(UploadUtil.publishImage(Item.FURNI_MEDIA, uploadFile, true));
                }
            }
        } else {
            // treat all other file types in the same manner, just publish them
            mediaInfos.add(new MediaInfo(hash, mimeType));
            UploadUtil.publishUploadFile(uploadFile);
        }

        // determine whether we're responding to the mchooser or a GWT upload
        Client client = Client.GWT;
        String cliStr = ctx.formFields.get("client");
        if (cliStr != null) {
            try {
                client = Client.valueOf(cliStr.toUpperCase());
            } catch (Exception e) {
                log.warning("Invalid client identifier [text=" + cliStr + ", error=" + e + "].");
            }
        }

        // now that we have published the file, post a response back to the caller
        sendResponse(ctx, client, mediaIds, mediaInfos);
    }

    /**
     * Send the response back to the caller.
     * @mediaIds media types to send back
     * @mediaInfos media hash/constraint/mimetype for each corresponding mediaId
     */
    protected void sendResponse (
        UploadContext ctx, Client client, List<String> mediaIds, List<MediaInfo> mediaInfos)
        throws IOException
    {
        // write out the magical incantations that are needed to cause our magical little
        // frame to communicate the newly assigned mediaHash to the ItemEditor widget
        PrintStream out = null;
        try {
            out = new PrintStream(ctx.rsp.getOutputStream());
            String filename = ctx.file.getName().replace("'", "").replace(" ", "");
            switch (client) {
            case GWT:
                out.println("<html>");
                out.println("<head></head>");
                String script = "";
                for (int ii = 0; ii < mediaIds.size() && ii < mediaInfos.size(); ii++) {
                    String mediaId = mediaIds.get(ii);
                    MediaInfo info = mediaInfos.get(ii);

                    // // this is one of the few places where we need to explicitly type
                    // // something as a CloudfrontMediaDesc, since we have to send the
                    // // precise signature and expiration through the damn JavaScript
                    // // layer, allowing GWT to reconstruct the CloudfrontMediaDesc on
                    // // the other side. Bah.
                    // CloudfrontMediaDesc desc = MediaDescFactory.createMediaDesc(
                    //     info.hash, info.mimeType, info.constraint);
                    script += "parent.setHash('" + mediaId + "', '" + filename +
                        "', '" + info.hash + "', " + info.mimeType + ", " + info.constraint +
                        // ", " + desc.getExpiration() + ", '" + desc.getSignature() + "'" +
                        ", " + info.width + ", " + info.height + ");";
                }
                out.println("<body onLoad=\"" + script + "\"></body>");
                out.println("</html>");
                break;

            case MCHOOSER:
                for (int ii = 0; ii < mediaIds.size() && ii < mediaInfos.size(); ii++) {
                    String mediaId = mediaIds.get(ii);
                    MediaInfo info = mediaInfos.get(ii);

                    // CloudfrontMediaDesc desc = MediaDescFactory.createMediaDesc(
                    //     info.hash, info.mimeType, info.constraint);
                    out.println(mediaId + " " + filename + " " + info.hash + " " +
                        info.mimeType + " " + info.constraint + " " +
                        // desc.getExpiration() + " " + desc.getSignature() + " " +
                        info.width + " " + info.height);
                }
                break;
            }

        } finally {
            StreamUtil.close(out);
        }
    }

    @Override // from AbstractUploadServlet
    protected int getMaxUploadSize ()
    {
        return LARGE_MEDIA_MAX_SIZE;
    }

    /**
     * Validate that the upload size is acceptable for the specified mimetype.
     */
    protected void validateFileLength (byte mimeType, int length)
        throws FileUploadException
    {
        long limit;
        switch (mimeType) {
        case MediaMimeTypes.AUDIO_MPEG:
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.VIDEO_MPEG:
        case MediaMimeTypes.VIDEO_QUICKTIME:
        case MediaMimeTypes.VIDEO_MSVIDEO:
        case MediaMimeTypes.APPLICATION_ZIP:
            limit = LARGE_MEDIA_MAX_SIZE;
            break;

        case MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH:
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

    /** Represents different potential upload clients. */
    protected enum Client { GWT, MCHOOSER };

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int SMALL_MEDIA_MAX_SIZE = 5 * MEGABYTE;
    protected static final int MEDIUM_MEDIA_MAX_SIZE = 10 * MEGABYTE;
    protected static final int LARGE_MEDIA_MAX_SIZE = 100 * MEGABYTE;
}
