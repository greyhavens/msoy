//
// $Id$

package client.shell;

import java.util.Date;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.fora.data.Comment;

/**
 * Displays a single comment.
 */
public class CommentPanel extends FlexTable
{
    public CommentPanel (final CommentsPanel parent, final Comment comment)
    {
        setStyleName("commentPanel");

        FlowPanel author = new FlowPanel();
        author.setStyleName("Author");
        author.add(Application.memberViewLink(comment.commentor));
        author.add(new InlineLabel(" said:"));
        setWidget(0, 0, author);

        setText(1, 0, comment.text);
        getFlexCellFormatter().setStyleName(1, 0, "Text");

        FlowPanel details = new FlowPanel();
        details.add(new InlineLabel(CShell.cmsgs.postedOn(_pfmt.format(new Date(comment.posted)))));
        setWidget(2, 0, details);
        getFlexCellFormatter().setStyleName(2, 0, "Posted");

        if (CShell.getMemberId() == comment.commentor.getMemberId() || CShell.isSupport()) {
            InlineLabel delete = new InlineLabel(CShell.cmsgs.deletePost(), false, true, false);
            delete.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    parent.deleteComment(CommentPanel.this, comment);
                    setText(2, 1, CShell.cmsgs.deletingPost());
                }
            });
            delete.addStyleName("actionLabel");
            details.add(delete);
        }
    }

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy h:mm:ss aa");
}
