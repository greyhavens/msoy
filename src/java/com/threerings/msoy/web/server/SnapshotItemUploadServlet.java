//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.ItemLogic;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

/**
 * Accepts an image upload and automatically creates an item for it.
 */
public class SnapshotItemUploadServlet extends AbstractSnapshotUploadServlet
{
    @Override
    protected void sendResponse (
        UploadContext ctx, Client client, String mediaId, MediaInfo info, MediaInfo tinfo)
        throws IOException
    {
        Photo image = new Photo();
        image.name = StringUtil.decode(ctx.formFields.get("name"));
        image.photoMedia = createMediaDesc(info);
        image.thumbMedia = createMediaDesc(tinfo);
        image.photoWidth = info.width;
        image.photoHeight = info.height;

        try {
            _itemLogic.createItem(ctx.memrec, image);
        } catch (ServiceException se) {
            System.err.println("Ruh-roh: " + se);
        }

        // TODO: some other response?
    }

    protected MediaDesc createMediaDesc (MediaInfo info)
    {
        MediaDesc desc = new MediaDesc();
        desc.hash = MediaDesc.stringToHash(info.hash);
        desc.mimeType = info.mimeType;
        desc.constraint = info.constraint;
        return desc;
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
}
