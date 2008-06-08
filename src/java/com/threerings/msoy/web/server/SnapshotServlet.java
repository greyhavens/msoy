//
// $Id$

package com.threerings.msoy.web.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Handles uploads of scene snapshot images.
 */
public class SnapshotServlet extends UploadServlet
{
    @Override // from UploadServlet
    protected void handleFileItems (FileItem file, FileItem[] allItems, int uploadLength,
                                    HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // note - no call to the superclass, this is a complete replacement!

        // pull out form data and validate it
        Map<String, String> fields = parseFormFields(allItems);
        int sceneId = validateAccess(fields, req);
        UploadFile uploadFile = new SnapshotUploadFile(file, sceneId);

        // some sanity checks
        if (! MediaDesc.isImage(uploadFile.getMimeType())) {
            throw new FileUploadException("Received snapshot file that is not an image [type=" +
                                          uploadFile.getMimeType() + "].");
        } else {
            log.info("Received snapshot: [type: " + file.getContentType() + ", size="
                     + file.getSize() + ", id=" + file.getFieldName() + "].");
        }
        validateFileLength(uploadFile.getMimeType(), uploadLength);

        // publish the file
        UploadUtil.publishSnapshot((SnapshotUploadFile) uploadFile);

        // and we're done!
    }

    @Override // from UploadServlet
    protected void accessDenied (HttpServletResponse rsp)
    {
        // in this version, we set the HTTP error code to something meaningful
        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        displayError(rsp, JavascriptError.ACCESS_DENIED);
    }

    /**
     * Validate that the uploading user is logged in, and owns the scene, and returns its sceneId.
     */
    protected int validateAccess (Map<String, String> formFields, HttpServletRequest req)
        throws AccessDeniedException
    {
        Integer sceneId = -1, memberId = -1;
        MemberRecord member = null;

        // did we get all the right data in the POST request?
        if (! formFields.containsKey("member") || ! formFields.containsKey("scene")) {
            throw new AccessDeniedException("Snapshot request is missing data.");
        }

        // pull out session token from the request header
        String token = CookieUtil.getCookieValue(req, "creds");
        if (token == null) {
            throw new AccessDeniedException("Must be logged in to upload screenshots.");
        }

        try {
            sceneId = Integer.decode(formFields.get("scene"));
            memberId = Integer.decode(formFields.get("member"));

            // make sure the user is authenticated, and pull out their record object
            WebIdent ident = new WebIdent(memberId, token);
            member = _mhelper.requireAuthedUser(ident);

            // now load the scene, and check who's the owner (todo: deal with group rooms)
            List<SceneBookmarkEntry> scenes = MsoyServer.sceneRepo.getOwnedScenes(memberId);
            for (SceneBookmarkEntry scene : scenes) {
                if (scene.sceneId == sceneId) {
                    return sceneId; // we found the owner - all done!
                }
            }
        } catch (Exception e) {
            throw new AccessDeniedException(
                "Could not confirm player access to scene [memberId=" + memberId + ", sceneId=" +
                sceneId + ", e=" + e + "].");
        }

        // scene is not owned; maybe the user is special?
        if (member.isSupport()) {
            log.info("Allowing support+ to upload a screenshot of another user's room [sceneId=" +
                     sceneId + ", memberId=" + memberId + "].");
            return sceneId;  // all done
        }

        // we've exhausted all possibilities
        throw new AccessDeniedException("User has no rights to upload a screenshot [sceneId=" +
                                        sceneId + ", memberId=" + memberId + "].");
    }

    /**
     * Converts all uploaded form fields into a map of field names to field values.
     */
    protected Map<String, String> parseFormFields (FileItem[] items)
    {
        Map<String, String> fields = Maps.newHashMap();
        for (FileItem item : items) {
            try {
                if (item.isFormField()) {
                    String name = item.getFieldName();
                    BufferedReader in =
                        new BufferedReader(new InputStreamReader(item.getInputStream()));
                    String value = in.readLine();
                    fields.put(name, value);
                }
            } catch (IOException ie) {
                // just skip this item
            }
        }
        return fields;
    }

    /** Provides useful member related services. */
    @Inject protected MemberHelper _mhelper;
}
