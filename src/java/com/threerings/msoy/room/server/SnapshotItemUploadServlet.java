//
// $Id$

package com.threerings.msoy.room.server;

import java.io.IOException;

import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.ItemLogic;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

/**
 * Accepts an image upload and automatically creates an item for it.
 */
public class SnapshotItemUploadServlet extends AbstractSnapshotUploadServlet
{
    @Override
    protected void sendResponse (UploadContext ctx, Client client, String mediaId, MediaInfo info,
                                 MediaInfo tinfo)
        throws IOException
    {
        Photo image = new Photo();
        image.name = StringUtil.decode(ctx.formFields.get("name"));
        image.photoMedia = createMediaDesc(info);
        image.thumbMedia = createMediaDesc(tinfo);
        image.photoWidth = info.width;
        image.photoHeight = info.height;

        try {
            _itemLogic.createItem(ctx.memrec.memberId, image);
        } catch (ServiceException se) {
            System.err.println("Ruh-roh: " + se);
        }

        // TODO: some other response?
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
}
