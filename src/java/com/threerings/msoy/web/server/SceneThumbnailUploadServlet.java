//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.fileupload.FileUploadException;
import com.google.inject.Inject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.AbstractSnapshotUploadServlet;
import com.threerings.msoy.room.server.SnapshotUploadFile;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import com.threerings.msoy.web.server.UploadUtil.CanonicalSnapshotInfo;

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

        // now, we need to make sure they have access to take the scene's canonical snapshot
        int sceneId = (Integer) ctx.data;

        try {
            SceneRecord scene = _sceneRepo.loadScene(sceneId);
            if (hasAccess(scene, ctx.memrec)) {
                return; // we're good to go!
            }
        } catch (Exception e) {
            throw new AccessDeniedException(
                "Could not confirm player access to scene [memberId=" + ctx.memrec.memberId +
                ", sceneId=" + sceneId + ", e=" + e + "].");
        }

        if (ctx.memrec.isSupport()) {
            log.info("Allowing support+ to upload a screenshot of another user's room",
                "sceneId", sceneId, "memberId", ctx.memrec.memberId);
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
            log.info("Received snapshot",
                "type", ctx.file.getContentType(), "size", ctx.file.getSize(),
                "id",ctx.file.getFieldName());
        }
        validateFileLength(uploadFile.getMimeType(), ctx.uploadLength);

        // publish the file, and we're done
        CanonicalSnapshotInfo info = UploadUtil.publishSnapshot((SnapshotUploadFile) uploadFile);

        _sceneRepo.setCanonicalImage(sceneId, info.canonical.hash, info.canonical.type,
            info.thumbnail.hash, info.thumbnail.type);

        // if scene was just published, record it to the feed now that we have the snapshot
        SceneRecord sceneRecord = _sceneRepo.loadScene(sceneId);
        Timestamp oneMinuteAgo = new Timestamp((new Date()).getTime() - 60 * 1000L);
        if (sceneRecord.lastPublished != null && sceneRecord.lastPublished.after(oneMinuteAgo)) {
            if (sceneRecord.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                _feedRepo.publishMemberMessage(sceneRecord.ownerId,
                    FeedMessageType.FRIEND_UPDATED_ROOM, String.valueOf(sceneId) + "\t" +
                        sceneRecord.name + "\t" + MediaDesc.mdToString(sceneRecord.getSnapshot()));
            } else {
                _feedRepo.publishGroupMessage(sceneRecord.ownerId,
                    FeedMessageType.GROUP_UPDATED_ROOM, String.valueOf(sceneId) + "\t" +
                        sceneRecord.name + "\t" + MediaDesc.mdToString(sceneRecord.getSnapshot()));
            }
        }
    }

    /**
     * Helper function for {@link #validateAccess}.
     */
    protected boolean hasAccess (SceneRecord scene, MemberRecord member)
    {
        if (scene == null) {
            return false;
        }

        switch (scene.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            return (scene.ownerId == member.memberId);

        case MsoySceneModel.OWNER_TYPE_GROUP: {
            return (_groupRepo.getRank(scene.ownerId, member.memberId) ==
                    GroupMembership.RANK_MANAGER);
        }

        default:
            log.warning("Can't determine access for unknown scene ownership type",
                        "sceneId", scene.sceneId, "otype", scene.ownerType);
            return false;
        }
    }

    // our dependencies
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
}
