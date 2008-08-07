//
// $Id$

package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import org.apache.commons.fileupload.FileUploadException;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.util.CookieUtil;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.server.AbstractUploadServlet;
import com.threerings.msoy.web.server.FileItemUploadFile;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.ServletWaiter;
import com.threerings.msoy.web.server.UploadFile;

import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;

public class SwiftlyUploadServlet extends AbstractUploadServlet
{
    // TODO: this servlet checks permissions its own special way, if we continue to keep
    // this, we should consider refactoring so that they work like the other upload servelets.

    @Override // from AbstractUploadServlet
    protected void handleFileItems (UploadContext ctx)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // wrap the FileItem in an UploadFile for publishing
        final UploadFile uploadFile = new FileItemUploadFile(ctx.file);

        // pull the session token from the request header
        String token = CookieUtil.getCookieValue(ctx.req, WebCreds.CREDS_COOKIE);
        if (token == null) {
            throw new AccessDeniedException("Must be logged in to upload item data.");
        }

        // attempt to extract the projectId from the field name
        String field = ctx.file.getFieldName();
        if (field == null) {
            throw new FileUploadException("Failed to extract form field from the upload request.");
        }

        // now pull the integers out
        final int projectId;
        try {
            projectId = Integer.parseInt(field);
        } catch (NumberFormatException nfe) {
            throw new FileUploadException(
                "Failed to parse integer form field parameters from the upload request. " +
                "[projectId=" + field + "]");
        }

        // verify this user is logged in and is a collaborator
        checkPermissions(token, projectId);

        log.info("Swiftly upload: [type=" + ctx.file.getContentType() +
                 ", size=" + ctx.file.getSize() + ", projectId=" + projectId + "].");

        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then commits the file to svn and adds it to the room
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("insertUploadFile[" + projectId + "]");
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _swiftlyMan.insertUploadFile(projectId, uploadFile, waiter);
            }
        });

        // block the servlet waiting for the dobject thread to complete the upload
        try {
            waiter.waitForResult();
        } catch (ServiceException se) {
            throw new FileUploadException(
                "Inserting the upload file into the room manager failed: " + se);
        }
    }

    @Override // from AbstractUploadServlet
    protected int getMaxUploadSize ()
    {
        return UPLOAD_MAX_SIZE;
    }

    /**
     * Verify the supplied member can perform the requested upload.
     */
    protected void checkPermissions (String authTok, int projectId)
        throws AccessDeniedException, FileUploadException
    {
        try {
            MemberRecord record = _mhelper.requireAuthedUser(authTok);
            if (!_swiftlyRepo.isCollaborator(projectId, record.memberId)) {
                throw new AccessDeniedException("Access denied. Not a collaborator: [memberId=" +
                    record.memberId + ", projectId=" + projectId + "]");
            }

        } catch (ServiceException e) {
            throw new AccessDeniedException("Access denied. Member not logged in " +
                                            "[token=" + authTok + ", projectId=" + projectId + "]");
        } catch (PersistenceException pe) {
            throw new FileUploadException("Failed when trying to check collaborator status");
        }
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected SwiftlyManager _swiftlyMan;
    @Inject protected MemberHelper _mhelper;
    @Inject protected SwiftlyRepository _swiftlyRepo;

    /** Restrict all Swiftly file uploads to 4 megabytes. */
    protected static final int UPLOAD_MAX_SIZE = 4 * MEGABYTE;
}
