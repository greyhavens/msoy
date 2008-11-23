//
// $Id$

package com.threerings.msoy.room.server;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.server.ItemMediaUploadServlet;

public abstract class AbstractSnapshotUploadServlet extends ItemMediaUploadServlet
{
    @Override
    protected void validateAccess (UploadContext ctx)
        throws AccessDeniedException
    {
        // did we get all the right data in the POST request?
        if (!ctx.formFields.containsKey("member") || !ctx.formFields.containsKey("scene")) {
            throw new AccessDeniedException("Snapshot request is missing data.");
        }

        try {
            ctx.data = Integer.decode(ctx.formFields.get("scene"));
        } catch (Exception e) {
            throw new AccessDeniedException("Could not parse sceneId");
        }

        // finally, do normal validation
        super.validateAccess(ctx);
    }

    @Override // from UploadServlet
    protected void accessDenied (HttpServletResponse rsp)
    {
        // in this version, we set the HTTP error code to something meaningful
        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        super.accessDenied(rsp);
    }

    /**
     * Convert a MediaDesc object from a Media info object. Width and Height from the MediaInfo
     * are ignored.
     */
    protected MediaDesc createMediaDesc (MediaInfo info)
    {
        MediaDesc desc = new MediaDesc();
        desc.hash = MediaDesc.stringToHash(info.hash);
        desc.mimeType = info.mimeType;
        desc.constraint = info.constraint;
        return desc;
    }
}
