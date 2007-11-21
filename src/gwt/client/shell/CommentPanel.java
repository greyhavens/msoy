//
// $Id$

package client.shell;

import java.util.Date;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.web.data.MemberCard;

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

    // @Override // from MessagePanel
    protected void addInfo (FlowPanel info)
    {
        super.addInfo(info);

        if (CShell.getMemberId() == _comment.commentor.getMemberId() || CShell.isSupport()) {
            InlineLabel delete = new InlineLabel(CShell.cmsgs.deletePost(), false, true, false);
            delete.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _parent.deleteComment(_comment, false);
                }
            });
            delete.addStyleName("Posted");
            delete.addStyleName("actionLabel");
            info.add(delete);
        }
    }

    protected CommentsPanel _parent;
    protected Comment _comment;
}
