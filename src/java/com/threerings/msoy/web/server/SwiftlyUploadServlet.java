//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.swiftly.server.ProjectRoomManager;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

public class SwiftlyUploadServlet extends AbstractUploadServlet
{
    @Override // from AbstractUploadServlet
    protected void handleUploadFile (final UploadFile uploadFile, int uploadLength,
                                     HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // attempt to extract the projectId and auth token from the field name
        String field = uploadFile.item.getFieldName();
        if (field == null) {
            throw new FileUploadException("Failed to extract form field from the upload request.");
        }
        
        // break the form field into the authentication components and the projectId
        String[] split = field.split("::");
        
        // first try to pull the authentication token out
        String token = split[0];
        if (StringUtil.isBlank(token)) {
            throw new FileUploadException(
                "Failed to parse the authentication token form field from the upload request.");
        }
        
        // now pull the integers out
        final int memberId;
        final int projectId;
        try {
            memberId = Integer.parseInt(split[1]);
            projectId = Integer.parseInt(split[2]);
            
        } catch (NumberFormatException nfe) {
            throw new FileUploadException(
                "Failed to parse integer form field parameters from the upload request.");
        }
        
        // finally, instantiate the WebIdent
        WebIdent ident = new WebIdent(memberId, token);

        // verify this user is logged in and is a collaborator
        checkPermissions(ident, projectId);

        log.info("Swiftly upload: [type: " + uploadFile.item.getContentType() + ", size="
            + uploadFile.item.getSize() + ", projectId=" + projectId + "].");

        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("insertUploadFile[" + projectId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {               
                ProjectRoomManager manager = MsoyServer.swiftlyMan.getRoomManager(projectId);
                if (manager == null) {
                    // TODO: is this at all clean?
                    waiter.requestFailed(
                        new Exception("No ProjectRoomManager found. Aborting upload."));
                }
                
                manager.insertUploadFile(uploadFile, waiter);
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
    protected void checkPermissions (WebIdent ident, int projectId)
        throws AccessDeniedException, FileUploadException
    {
        try {
            MemberRecord record = MsoyServiceServlet.requireAuthedUser(ident);
            if (!MsoyServer.swiftlyRepo.isCollaborator(projectId, record.memberId)) {
                throw new AccessDeniedException();
            }

        } catch (ServiceException e) {
            throw new AccessDeniedException();
            
        } catch (PersistenceException pe) {
            throw new FileUploadException("Failed when trying to check collaborator status");
        }
    }

    /** Restrict all Swiftly file uploads to 10 megabytes. */
    protected static final int UPLOAD_MAX_SIZE = 10 * MEGABYTE;
}
