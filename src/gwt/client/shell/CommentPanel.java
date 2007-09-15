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

import client.util.MsoyUI;

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

        setText(2, 0, "on " + _pfmt.format(new Date(comment.posted)));
        getFlexCellFormatter().setStyleName(2, 0, "Posted");

        if (CShell.getMemberId() == comment.commentor.getMemberId() || CShell.isSupport()) {
            getFlexCellFormatter().setColSpan(0, 0, 2);
            getFlexCellFormatter().setColSpan(1, 0, 2);
            getFlexCellFormatter().setStyleName(2, 1, "Posted");
            setWidget(2, 1, MsoyUI.createActionLabel("[delete]", new ClickListener() {
                public void onClick (Widget sender) {
                    parent.deleteComment(CommentPanel.this, comment);
                    setText(2, 1, "[deleting...]");
                }
            }));
        }
    }

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy h:mm:ss aa");
}
