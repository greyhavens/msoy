//
// $Id$

package com.threerings.msoy.room.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.ItemLogic;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

/**
 * Accepts an image upload and automatically creates an item for it.
 */
public class SnapshotItemUploadServlet extends AbstractSnapshotUploadServlet
{
    @Override
    protected void sendResponse (UploadContext ctx, Client client,
                                 List<String> mediaIds, List<MediaInfo> mediaInfos)
        throws IOException
    {
        // always make the item, unless makeItem=false
        if (!"false".equals(ctx.formFields.get("makeItem"))) {
            Photo image = new Photo();
            image.name = StringUtil.decode(ctx.formFields.get("name"));

            for (int ii = 0; ii < mediaIds.size(); ii++) {
                String mediaId = mediaIds.get(ii);
                MediaInfo info = mediaInfos.get(ii);
                if (Item.THUMB_MEDIA.equals(mediaId)) {
                    image.setThumbnailMedia(createMediaDesc(info));
                } else if (Item.FURNI_MEDIA.equals(mediaId)) {
                    image.setFurniMedia(createMediaDesc(info));
                } else {
                    image.photoMedia = createMediaDesc(info);
                    image.photoWidth = info.width;
                    image.photoHeight = info.height;
                }
            }

            try {
                _itemLogic.createItem(ctx.memrec.memberId, image);
            } catch (ServiceException se) {
                System.err.println("Ruh-roh: " + se);
            }
        }

        PrintStream out = new PrintStream(ctx.rsp.getOutputStream());
        try {
            int snapIdx = mediaIds.indexOf("snapshot");
            if (snapIdx != -1) {
                MediaInfo info = mediaInfos.get(snapIdx);
                out.println(DeploymentConfig.mediaURL +
                    info.hash + MediaDesc.mimeTypeToSuffix(info.mimeType));
            }
        } finally {
            StreamUtil.close(out);
        }
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
}
