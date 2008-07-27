//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;

import com.google.inject.Inject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.world.server.AbstractSnapshotUploadServlet;
import com.threerings.msoy.world.server.SnapshotUploadFile;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Handles uploads of canonical scene snapshot images.
 */
public class SceneThumbnailUploadServlet extends AbstractSnapshotUploadServlet
{
    @Override
    protected void validateAccess (UploadContext ctx)
        throws AccessDeniedException
    {
        super.validateAccess(ctx);

        // now, we need to make sure they have access to take the scene's canonical snapshot.
        int sceneId = (Integer) ctx.data;

        try {
            // TODO: just check the one scene, not load them all, ffs
            // now load the scene, and check who's the owner (todo: deal with group rooms)
            List<SceneBookmarkEntry> scenes = _sceneRepo.getOwnedScenes(ctx.memrec.memberId);
            for (SceneBookmarkEntry scene : scenes) {
                if (scene.sceneId == sceneId) {
                    return; // we're good to go!
                }
            }
        } catch (Exception e) {
            throw new AccessDeniedException(
                "Could not confirm player access to scene [memberId=" + ctx.memrec.memberId +
                ", sceneId=" + sceneId + ", e=" + e + "].");
        }

        // scene is not owned; maybe the user is special?
        if (ctx.memrec.isSupport()) {
            log.info("Allowing support+ to upload a screenshot of another user's room [sceneId=" +
                     sceneId + ", memberId=" + ctx.memrec.memberId + "].");
            return; // we're good to go!
        }

        // we've exhausted all possibilities
        throw new AccessDeniedException("User has no rights to upload a screenshot [sceneId=" +
                                        sceneId + ", memberId=" + ctx.memrec.memberId + "].");
    }

    @Override // from UploadServlet
    protected void handleFileItems (UploadContext ctx)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // note - no call to the superclass, this is a complete replacement!

        // pull out form data and validate it
        int sceneId = (Integer) ctx.data;
        UploadFile uploadFile = new SnapshotUploadFile(ctx.file, sceneId);

        // some sanity checks
        if (!MediaDesc.isImage(uploadFile.getMimeType())) {
            throw new FileUploadException("Received snapshot file that is not an image [type=" +
                                          uploadFile.getMimeType() + "].");
        } else {
            log.info("Received snapshot: [type: " + ctx.file.getContentType() + ", size="
                     + ctx.file.getSize() + ", id=" + ctx.file.getFieldName() + "].");
        }
        validateFileLength(uploadFile.getMimeType(), ctx.uploadLength);

        // publish the file, and we're done
        UploadUtil.publishSnapshot((SnapshotUploadFile) uploadFile);
    }

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
}
