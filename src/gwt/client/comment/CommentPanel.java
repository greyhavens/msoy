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
import com.threerings.msoy.web.data.MemberCard;

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

        MemberCard card = new MemberCard();
        card.name = comment.commentor;
        card.photo = comment.photo;
        setMessage(card, new Date(comment.posted), comment.text);
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

    @Override // from MessagePanel
    protected int getThumbnailSize ()
    {
        return _parent.getThumbnailSize();
    }

    protected CommentsPanel _parent;
    protected Comment _comment;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
