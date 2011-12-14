//
// $Id$

package client.comment;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.web.gwt.MemberCard;

import client.images.msgs.MsgsImages;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MessagePanel;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.ThumbBox;
import client.util.InfoCallback;

/**
 * Displays a single comment.
 */
public class CommentPanel extends MessagePanel
{
    public CommentPanel (CommentsPanel parent, Comment comment)
    {
        this(parent, comment, null);
    }

    public CommentPanel (CommentsPanel parent, Comment comment, Widget authorBits)
    {
        _parent = parent;
        _comment = comment;
        _authorBits = authorBits;

        _rated = false;

        _displayed = _parent.commentsCanBeRated() ? _parent.shouldDisplay(_comment) : true;

        addStyleName("commentPanel");
        addStyleName(_comment.isReply() ? "Reply" : "Subject");

        updateComment();
    }

    @Override // from MessagePanel
    protected void addAuthorInfo (FlowPanel info)
    {
        if (_authorBits != null) {
            info.add(_authorBits);
        }
    }

    @Override // from MessagePanel
    protected void addInfo (FlowPanel info)
    {
        super.addInfo(info);

        if (!_comment.isReply()) {
            InlineLabel reply = new InlineLabel(_cmsgs.replyToPost(), false, true, false);
            reply.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _parent.showPostPopup(_comment);
                }
            });
            reply.addStyleName("Posted");
            reply.addStyleName("actionLabel");
            info.add(reply);
        }

        if (_parent.canDelete(_comment)) {
            InlineLabel delete = new InlineLabel(_cmsgs.deletePost(), false, true, false);
            delete.addClickHandler(new PromptPopup(_cmsgs.deletePostConfirm(),
                                                    _parent.deleteComment(_comment)).
                                    setContext("\"" + _comment.text + "\""));
            delete.addStyleName("Posted");
            delete.addStyleName("actionLabel");
            info.add(delete);
        }

        if (_parent.canComplain(_comment)) {
            InlineLabel complain = new InlineLabel(_cmsgs.complainPost(), false, true, false);
            complain.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
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
        if (_parent.commentsCanBeRated()) {
            addRatingUI(tools);
        }
        if (_parent.commentsCanBeBatchDeleted()) {
            _delBox = new CheckBox();
            tools.add(_delBox);
            _delBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override public void onValueChange (ValueChangeEvent<Boolean> event) {
                    _parent.setDeletionCheckbox(_comment, event.getValue());
                }
            });
        }
        return tools;
    }

    protected void addRatingUI (InlinePanel tools)
    {
        int adjustedRating = _comment.currentRating - 1;
        if (adjustedRating != 0) {
            String plus = (adjustedRating > 0) ? "+" : "";
            InlineLabel rating = new InlineLabel(plus + adjustedRating, false, true, false);
            rating.addStyleName("Posted");
            tools.add(rating);
        }

        if (!_displayed) {
            InlineLabel showComment = new InlineLabel(_cmsgs.showComment(), false, true, false);
            showComment.addStyleName("Posted");
            showComment.addStyleName("actionLabel");
            showComment.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _displayed = true;
                    _rated = false;
                    updateComment();
                }
            });
            tools.add(showComment);
            return;
        }

        if (_rated || !CShell.isValidated() ||
                CShell.getMemberId() == _comment.commentor.getId()) {
            return;
        }

        _upRate = makeThumbButton(_images.thumb_up_default(), _images.thumb_up_over(),
            _cmsgs.upComment(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    rateComment(true);
                }
            });
        tools.add(_upRate);

        _downRate = makeThumbButton(_images.thumb_down_default(), _images.thumb_down_over(),
            _cmsgs.downComment(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    rateComment(false);
                }
            });
        tools.add(_downRate);
    }

    protected void rateComment (boolean rating)
    {
        _upRate.setEnabled(false);
        _downRate.setEnabled(false);
        _parent.rateComment(_comment, rating, new InfoCallback<Integer>() {
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
        card.role = _comment.role;

        if (_parent.shouldEmphasize(_comment)) {
            addStyleName("emphasized");
        } else {
            removeStyleName("emphasized");
        }

        if (_displayed) {
            setMessage(card, new Date(_comment.posted), _comment.text);
        } else {
            setMessage(card, new Date(_comment.posted), null);
        }
    }

    @Override // from MessagePanel
    protected int getThumbnailSize ()
    {
        return _parent.getThumbnailSize();
    }

    protected static PushButton makeThumbButton (
        ImageResource def, ImageResource over, String tip, ClickHandler onClick)
    {
        Image defImg = new Image(def);
        defImg.addStyleName("inline");
        defImg.setTitle(tip);

        Image overImg = new Image(over);
        overImg.addStyleName("inline");
        overImg.setTitle(tip);

        PushButton button = MsoyUI.createPushButton(defImg, overImg, overImg, onClick);
        button.addStyleName("ActionIcon");
        return button;
    }

    protected CommentsPanel _parent;
    protected Comment _comment;
    protected Widget _authorBits;

    protected boolean _displayed;
    protected boolean _rated;
    protected PushButton _upRate, _downRate;
    protected CheckBox _delBox;

    protected static final MsgsImages _images = GWT.create(MsgsImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
