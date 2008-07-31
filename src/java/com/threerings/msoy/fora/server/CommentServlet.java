//
// $Id$

package com.threerings.msoy.fora.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.fora.gwt.Comment;
import com.threerings.msoy.fora.gwt.CommentService;
import com.threerings.msoy.fora.server.persist.CommentRecord;
import com.threerings.msoy.fora.server.persist.CommentRepository;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.underwire.server.SupportLogic;

import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;
import com.threerings.msoy.world.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CommentService}.
 */
public class CommentServlet extends MsoyServiceServlet
    implements CommentService
{
    // from interface CommentService
    public CommentResult loadComments (int etype, int eid, int offset, int count, boolean needCount)
        throws ServiceException
    {
        // no authentication required to view comments
        try {
            List<CommentRecord> records = _commentRepo.loadComments(etype, eid, offset, count);

            // resolve the member cards for all commentors
            IntMap<MemberCard> cards = new HashIntMap<MemberCard>();
            ArrayIntSet memIds = new ArrayIntSet();
            for (CommentRecord record : records) {
                memIds.add(record.memberId);
            }
            for (MemberCardRecord mcrec : _memberRepo.loadMemberCards(memIds)) {
                cards.put(mcrec.memberId, mcrec.toMemberCard());
            }

            // convert the comment records to runtime records
            List<Comment> comments = Lists.newArrayList();
            for (CommentRecord record : records) {
                Comment comment = record.toComment(cards);
                if (comment.commentor == null) {
                    continue; // this member was deleted, shouldn't happen
                }
                comments.add(comment);
            }

            // prepare and deliver the final result
            CommentResult result = new CommentResult();
            result.comments = comments;
            if (needCount) {
                result.commentCount = (records.size() < count && offset == 0) ?
                    records.size() : _commentRepo.loadCommentCount(etype, eid);
            }

            return result;

        } catch (PersistenceException pe) {
            log.warning("Failed to load comments [entity=" + etype + ":" + eid +
                    ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface CommentService
    public Comment postComment (WebIdent ident, int etype, int eid, String text)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        // validate the entity type and id (sort of; we can't *really* validate the id without a
        // bunch of entity specific befuckery which I don't particularly care to do)
        if (!Comment.isValidType(etype) || eid == 0) {
            log.warning("Refusing to post comment on illegal entity [entity=" + etype + ":" + eid +
                        ", who=" + mrec.who() +
                        ", text=" + StringUtil.truncate(text, 40, "...") + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            // record the comment to the data-ma-base
            _commentRepo.postComment(etype, eid, mrec.memberId, text);

            // find out the owner id of and the entity name for the entity that was commented on
            int ownerId = 0;
            String entityName = null;

            // if this is a comment on a user room, post a self feed message
            if (etype == Comment.TYPE_ROOM) {
                SceneRecord scene = _sceneRepo.loadScene(eid);
                if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    String data = scene.sceneId + "\t" + scene.name;
                    _feedRepo.publishSelfMessage(
                        scene.ownerId,  mrec.memberId, FeedMessageType.SELF_ROOM_COMMENT, data);
                    ownerId = scene.ownerId;
                    entityName = scene.name;
                }

                // update the member's WHIRLED_COMMENTS stat
                _statLogic.incrementStat(mrec.memberId, StatType.WHIRLED_COMMENTS, 1);

            } else if (etype == Comment.TYPE_PROFILE_WALL) {
                ownerId = eid;

            } else {
                try {
                    ItemRepository<?> repo = _itemLogic.getRepository((byte)etype);
                    CatalogRecord listing = repo.loadListing(eid, true);
                    if (listing != null) {
                        ItemRecord item = listing.item;
                        if (item != null) {
                            ownerId = item.creatorId;
                            entityName = item.name;
                        }
                    }

                } catch (ServiceException se) {
                    // this is merely a failure to send the notification, don't let this
                    // exception make it back to the commenter
                    log.warning(
                        "unable to locate repository for apparent item type [" + etype + "]");

                } catch (PersistenceException pe) {
                    log.warning("unable to load the item commented on [" + etype + ", " + eid +
                        ", " + pe + "]");
                }
            }

            // notify the item creator that a comment was made
            if (ownerId > 0 && ownerId != mrec.memberId) {
                _notifyMan.notifyEntityCommented(ownerId, etype, eid, entityName);
            }

            // fake up a comment record to return to the client
            Comment comment = new Comment();
            comment.commentor = mrec.getName();
            comment.posted = System.currentTimeMillis();
            comment.text = text;
            return comment;

        } catch (PersistenceException pe) {
            log.warning("Failed to post comment [entity=" + etype + ":" + eid +
                    ", who=" + mrec.who() +
                    ", text=" + StringUtil.truncate(text, 40, "...") + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface CommentService
    public boolean deleteComment (WebIdent ident, int etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            // if we're not support personel, ensure that we are the poster of this comment
            if (!mrec.isSupport()) {
                CommentRecord record = _commentRepo.loadComment(etype, eid, posted);
                if (record == null ||
                    !Comment.canDelete(etype, eid, record.memberId, mrec.memberId)) {
                    return false;
                }
            }

            _commentRepo.deleteComment(etype, eid, posted);
            return true;

        } catch (PersistenceException pe) {
            log.warning("Failed to delete comment [entity=" + etype + ":" + eid +
                    ", who=" + mrec.who() + ", posted=" + posted + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface CommentService
    public void complainComment (WebIdent ident, String subject, int etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            CommentRecord record = _commentRepo.loadComment(etype, eid, posted);
            if (record == null) {
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            String link = ServerConfig.getServerURL();
            if (etype >= Comment.TYPE_ITEM_MIN && etype <= Comment.TYPE_ITEM_MAX) {
                link += "/#shop-l_" + etype + "_" + eid;
            } else if (etype == Comment.TYPE_ROOM) {
                link += "/#world-room_" + eid;
            } else {
                link = null;
            }

            _supportLogic.addMessageComplaint(
                mrec.getName(), record.memberId, record.text, subject, link);

        } catch (PersistenceException pe) {
            log.warning("Failed to complain comment [entity=" + etype + ":" + eid +
                    ", who=" + mrec.who() + ", posted=" + posted + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected StatLogic _statLogic;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected CommentRepository _commentRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
