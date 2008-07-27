//
// $Id$

package com.threerings.msoy.world.server;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.UploadServlet;

public abstract class AbstractSnapshotUploadServlet extends UploadServlet
{
    @Override
    protected void validateAccess (UploadContext ctx)
        throws AccessDeniedException
    {
        Integer sceneId = -1;
        Integer memberId = -1;

        // did we get all the right data in the POST request?
        if (!ctx.formFields.containsKey("member") || !ctx.formFields.containsKey("scene")) {
            throw new AccessDeniedException("Snapshot request is missing data.");
        }

        // pull out session token from the request header
        String token = CookieUtil.getCookieValue(ctx.req, "creds");
        if (token == null) {
            throw new AccessDeniedException("Must be logged in to upload screenshots.");
        }

        try {
            memberId = Integer.decode(ctx.formFields.get("member"));
            sceneId = Integer.decode(ctx.formFields.get("scene"));

            // make sure the user is authenticated, and pull out their record object
            WebIdent ident = new WebIdent(memberId, token);

            // ok! They're validated!
            ctx.memrec = _mhelper.requireAuthedUser(ident);
            ctx.data = sceneId;

        } catch (Exception e) {
            throw new AccessDeniedException(
                "Could not confirm player access to scene [memberId=" + memberId + ", sceneId=" +
                sceneId + ", e=" + e + "].");
        }
    }

    @Override // from UploadServlet
    protected void accessDenied (HttpServletResponse rsp)
    {
        // in this version, we set the HTTP error code to something meaningful
        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        super.accessDenied(rsp);
    }

    // our dependencies
    @Inject protected MemberHelper _mhelper;
}
