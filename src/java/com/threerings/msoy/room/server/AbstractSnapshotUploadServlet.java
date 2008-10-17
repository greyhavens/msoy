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

        // pull out session token from the request header
        String token = CookieUtil.getCookieValue(ctx.req, WebCreds.credsCookie());
        if (token == null) {
            throw new AccessDeniedException("Must be logged in to upload screenshots.");
        }

        Integer sceneId = -1;
        try {
            sceneId = Integer.decode(ctx.formFields.get("scene"));
            // make sure the user is authenticated, and pull out their record object
            ctx.memrec = _mhelper.requireAuthedUser(token);
            ctx.data = sceneId;

        } catch (Exception e) {
            throw new AccessDeniedException("Could not confirm player access to scene " +
                                            "[sceneId=" + sceneId + ", e=" + e + "].");
        }
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

    // our dependencies
    @Inject protected MemberHelper _mhelper;
}
