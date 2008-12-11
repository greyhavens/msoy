//
// $Id$

package client.comment;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.web.gwt.MemberCard;

import client.shell.ShellMessages;
import client.ui.MessagePanel;
import client.ui.PromptPopup;

/**
 * Displays a single comment.
 */
public class CommentPanel extends MessagePanel
{
    public CommentPanel (CommentsPanel parent, Comment comment)
    {
        _parent = parent;
        _comment = comment;

        _displayComment = _parent.shouldDisplay(_comment);
        updateComment();
    }

    @Override // from MessagePanel
    protected void addInfo (FlowPanel info)
    {
        super.addInfo(info);

        if (_parent.canDelete(_comment)) {
            InlineLabel delete = new InlineLabel(_cmsgs.deletePost(), false, true, false);
            delete.addClickListener(new PromptPopup(_cmsgs.deletePostConfirm(),
                                                    _parent.deleteComment(_comment)).
                                    setContext("\"" + _comment.text + "\""));
            delete.addStyleName("Posted");
            delete.addStyleName("actionLabel");
            info.add(delete);
        }

        if (_parent.canComplain(_comment)) {
            InlineLabel complain = new InlineLabel(_cmsgs.complainPost(), false, true, false);
            complain.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _parent.complainComment(_comment);
                }
            });
            complain.addStyleName("Posted");
            complain.addStyleName("actionLabel");
            info.add(complain);
        }

//        InlineLabel rating = new InlineLabel(
//            _cmsgs.rating("" + _comment.currentRating), false, true, false);
//        rating.addStyleName("Posted");
//        info.add(rating);

        if (!_displayComment) {
            InlineLabel showComment = new InlineLabel(_cmsgs.showComment(), false, true, false);
            showComment.addStyleName("Posted");
            showComment.addStyleName("actionLabel");
            showComment.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _displayComment = true;
                    updateComment();
                }
            });
            info.add(showComment);

//        } else if (_comment.myRating == Comment.RATED_NONE) {
//            InlineLabel upRate = new InlineLabel("[+]", false, false, false);
//            upRate.addStyleName("Posted");
//            upRate.addStyleName("actionLabel");
//            upRate.addClickListener(new ClickListener() {
//                public void onClick (Widget sender) {
//                    _parent.rateComment(_comment, true);
//
//                    _comment.currentRating += 1;
//                    _comment.totalRatings ++;
//                    _comment.myRating = Comment.RATED_UP;
//
//                    updateComment();
//                }
//            });
//            info.add(upRate);
//
//            InlineLabel downRate = new InlineLabel("[-]", false, false, false);
//            downRate.addStyleName("Posted");
//            downRate.addStyleName("actionLabel");
//            downRate.addClickListener(new ClickListener() {
//                public void onClick (Widget sender) {
//                    _parent.rateComment(_comment, false);
//
//                    _comment.currentRating -= 1;
//                    _comment.totalRatings ++;
//                    _comment.myRating = Comment.RATED_DOWN;
//
//                    updateComment();
//                }
//            });
//            info.add(downRate);
        }

    }

    protected void updateComment ()
    {
        MemberCard card = new MemberCard();
        card.name = _comment.commentor;
        card.photo = _comment.photo;

        if (_displayComment) {
            setMessage(card, new Date(_comment.posted), _comment.text);
        } else {
            // TODO
            setMessage(card, new Date(_comment.posted), "");
        }
    }

    @Override // from MessagePanel
    protected int getThumbnailSize ()
    {
        return _parent.getThumbnailSize();
    }

    protected CommentsPanel _parent;
    protected Comment _comment;

    protected boolean _displayComment;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
