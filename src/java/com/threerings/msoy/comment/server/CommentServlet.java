//
// $Id$

package com.threerings.msoy.comment.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.depot.DatabaseException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.underwire.server.SupportLogic;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.server.persist.CommentRecord;
import com.threerings.msoy.comment.server.persist.CommentRepository;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

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
        List<CommentRecord> records = _commentRepo.loadComments(etype, eid, offset, count, false);

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
    }

    // from interface CommentService
    public Comment postComment (int etype, int eid, String text)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // validate the entity type and id (sort of; we can't *really* validate the id without a
        // bunch of entity specific befuckery which I don't particularly care to do)
        if (!isValidType(etype) || eid == 0) {
            log.warning("Refusing to post comment on illegal entity", "type", etype, "id", eid,
                        "who", mrec.who(), "text",  StringUtil.truncate(text, 40, "..."));
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // record the comment to the data-ma-base
        CommentRecord crec = _commentRepo.postComment(etype, eid, mrec.memberId, text);

        // find out the owner id of and the entity name for the entity that was commented on
        int ownerId = 0;
        String entityName = null;

        // if this is a comment on a user room, post a self feed message
        if (etype == Comment.TYPE_ROOM) {
            SceneRecord scene = _sceneRepo.loadScene(eid);
            if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                String data = scene.sceneId + "\t" + scene.name + "\t"
                    + MediaDesc.mdToString(scene.getSnapshot());
                _feedRepo.publishSelfMessage(
                    scene.ownerId,  mrec.memberId, FeedMessageType.SELF_ROOM_COMMENT, data);
                ownerId = scene.ownerId;
                entityName = scene.name;
            }

        } else if (etype == Comment.TYPE_PROFILE_WALL) {
            ownerId = eid;

        // comment on an item
        } else {
            try {
                ItemRepository<?> repo = _itemLogic.getRepository((byte)etype);
                CatalogRecord listing = repo.loadListing(eid, true);
                if (listing != null) {
                    ItemRecord item = listing.item;
                    if (item != null) {
                        ownerId = item.creatorId;
                        entityName = item.name;

                        // when commenting on a listed item, post a self feed message
                        String data = item.getType() + "\t" + listing.catalogId + "\t"
                            + item.name + "\t" + MediaDesc.mdToString(item.getThumbMediaDesc());
                        _feedRepo.publishSelfMessage(ownerId, mrec.memberId,
                            FeedMessageType.SELF_ITEM_COMMENT, data);
                    }
                }

            } catch (ServiceException se) {
                // this is merely a failure to send the notification, don't let this exception
                // make it back to the commenter
                log.warning("Unable to locate repository for apparent item type", "type", etype);

            } catch (DatabaseException de) {
                log.warning("Unable to load comment target item", "type", etype, "id", eid, de);
            }
        }

        // notify the item creator that a comment was made
        if (ownerId > 0 && ownerId != mrec.memberId) {
            _notifyMan.notifyEntityCommented(ownerId, etype, eid, entityName);
        }

        // convert the record to a runtime record to return to the caller
        Comment comment = crec.toComment(Collections.<Integer, MemberCard>emptyMap());
        comment.commentor = mrec.getName();
        return comment;
    }

    // from interface CommentService
    public int rateComment (int etype, int eid, long posted, boolean rating)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        return _commentRepo.rateComment(etype, eid, posted, mrec.memberId, rating);
    }

    // from interface CommentService
    public boolean deleteComment (int etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
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
    }

    // from interface CommentService
    public void complainComment (String subject, int etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        CommentRecord record = _commentRepo.loadComment(etype, eid, posted);
        if (record == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        String link = ServerConfig.getServerURL();
        if (etype >= Comment.TYPE_ITEM_MIN && etype <= Comment.TYPE_ITEM_MAX) {
            link += "#shop-l_" + etype + "_" + eid;
        } else if (etype == Comment.TYPE_ROOM) {
            link += "#world-room_" + eid;
        } else {
            link = null;
        }

        _supportLogic.addMessageComplaint(
            mrec.getName(), record.memberId, record.text, subject, link);
    }

    /**
     * Returns true if this is a valid comment entity type, false if not.
     */
    protected static boolean isValidType (int entityType)
    {
        // if it's an item, we must delegate to the Item class
        if (entityType >= Comment.TYPE_ITEM_MIN && entityType <= Comment.TYPE_ITEM_MAX) {
            return Item.getClassForType((byte)entityType) != null;
        }

        // otherwise make sure we have a constant defined for this type
        switch (entityType) {
        case Comment.TYPE_ROOM:
        case Comment.TYPE_PROFILE_WALL:
            return true;
        default:
            return false;
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
