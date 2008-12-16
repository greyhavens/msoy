//
// $Id$

package client.comment;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.web.gwt.MemberCard;

import client.images.msgs.MsgsImages;
import client.shell.ShellMessages;
import client.ui.MessagePanel;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.ThumbBox;
import client.util.MsoyCallback;

/**
 * Displays a single comment.
 */
public class CommentPanel extends MessagePanel
{
    public CommentPanel (CommentsPanel parent, Comment comment)
    {
        _parent = parent;
        _comment = comment;

        _rated = false;
        _displayed = _parent.shouldDisplay(_comment);

        addStyleName("commentPanel");

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
    }

    @Override
    protected ThumbBox getThumbBox (MemberCard poster)
    {
        // don't show an icon for hidden messages
        return _displayed ? super.getThumbBox(poster) : null;
    }

    @Override
    protected Panel getTools ()
    {
        InlinePanel tools = new InlinePanel("Tools");

        InlineLabel rating = new InlineLabel("" + _comment.currentRating, false, true, false);
        rating.addStyleName("Posted");
        tools.add(rating);

        if (!_displayed) {
            InlineLabel showComment = new InlineLabel(_cmsgs.showComment(), false, true, false);
            showComment.addStyleName("Posted");
            showComment.addStyleName("actionLabel");
            showComment.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _displayed = true;
                    _rated = false;
                    updateComment();
                }
            });
            tools.add(showComment);
            return tools;
        }

        if (_rated) {
            return tools;
        }

        _upRate = makeThumbButton(_images.thumb_up_default(), _images.thumb_up_over(),
            _cmsgs.upComment(), new ClickListener() {
                public void onClick (Widget sender) {
                    rateComment(true);
                }
            });
        tools.add(_upRate);

        _downRate = makeThumbButton(_images.thumb_down_default(), _images.thumb_down_over(),
            _cmsgs.downComment(), new ClickListener() {
                public void onClick (Widget sender) {
                    rateComment(false);
                }
            });
        tools.add(_downRate);

        return tools;
    }

    protected void rateComment (boolean rating)
    {
        _upRate.setEnabled(false);
        _downRate.setEnabled(false);
        _parent.rateComment(_comment, rating, new MsoyCallback<Integer>() {
            public void onSuccess (Integer adjustment) {
                _comment.currentRating += adjustment;
                // if this was not a re-rating, the sum will have gone up or down by 1
                if (adjustment == -1 || adjustment == 1) {
                    _comment.totalRatings++;
                }
                _rated = true;
                _displayed = _parent.shouldDisplay(_comment);
                updateComment();
            }
        });
    }

    protected void updateComment ()
    {
        MemberCard card = new MemberCard();
        card.name = _comment.commentor;
        card.photo = _comment.photo;

        if (_displayed) {
            setMessage(card, new Date(_comment.posted), _comment.text);
        } else {
            // TODO
            setMessage(card, new Date(_comment.posted), null);
        }
    }

    @Override // from MessagePanel
    protected int getThumbnailSize ()
    {
        return _parent.getThumbnailSize();
    }

    protected static PushButton makeThumbButton (
        AbstractImagePrototype def, AbstractImagePrototype over, String tip, ClickListener onClick)
    {
        Image defImg = def.createImage();
        defImg.addStyleName("inline");
        defImg.setTitle(tip);

        Image overImg = over.createImage();
        overImg.addStyleName("inline");
        overImg.setTitle(tip);

        PushButton button = MsoyUI.createPushButton(defImg, overImg, overImg, onClick);
        button.addStyleName("ActionIcon");
        return button;
    }

    protected CommentsPanel _parent;
    protected Comment _comment;

    protected boolean _displayed;
    protected boolean _rated;
    protected PushButton _upRate, _downRate;

    protected static final MsgsImages _images = GWT.create(MsgsImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
