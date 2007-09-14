//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.fora.server.persist.CommentRecord;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.CommentService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CommentService}.
 */
public class CommentServlet extends MsoyServiceServlet
    implements CommentService
{
    // from interface CommentService
    public List loadComments (int etype, int eid, int offset, int count)
        throws ServiceException
    {
        // no authentication required to view comments
        try {
            return MsoyServer.commentRepo.loadComments(etype, eid, offset, count);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load comments [entity=" + etype + ":" + eid +
                    ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface CommentService
    public Comment postComment (WebIdent ident, int etype, int eid, String text)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // validate the entity type and id (sort of; we can't *really* validate the id without a
        // bunch of entity specific befuckery which I don't particularly care to do)
        if (!Comment.isValidType(etype) || eid == 0) {
            log.warning("Refusing to post comment on illegal entity [entity=" + etype + ":" + eid +
                        ", who=" + mrec.getName() +
                        ", text=" + StringUtil.truncate(text, 40, "...") + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        try {
            // record the comment to the data-ma-base
            MsoyServer.commentRepo.postComment(etype, eid, mrec.memberId, text);

            // fake up a comment record to return to the client
            Comment comment = new Comment();
            comment.commentor = mrec.getName();
            comment.posted = System.currentTimeMillis();
            comment.text = text;
            return comment;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to post comment [entity=" + etype + ":" + eid +
                    ", who=" + mrec.getName() +
                    ", text=" + StringUtil.truncate(text, 40, "...") + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface CommentService
    public boolean deleteComment (WebIdent ident, int etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        try {
            // if we're not support personel, ensure that we are the poster of this comment
//             if (!mrec.isSupport()) {
                CommentRecord record = MsoyServer.commentRepo.loadComment(etype, eid, posted);
                if (record == null || record.memberId != mrec.memberId) {
                    return false;
                }
//             }

            MsoyServer.commentRepo.deleteComment(etype, eid, posted);
            return true;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to delete comment [entity=" + etype + ":" + eid +
                    ", who=" + mrec.getName() + ", posted=" + posted + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }
}
