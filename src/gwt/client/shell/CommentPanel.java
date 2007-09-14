//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.fora.data.Comment;

import java.util.Date;

import client.util.MsoyUI;

/**
 * Displays a single comment.
 */
public class CommentPanel extends VerticalPanel
{
    public CommentPanel (Comment comment)
    {
        setStyleName("commentPanel");

        FlowPanel author = new FlowPanel();
        author.setStyleName("Author");
        author.add(Application.memberViewLink(comment.commentor));
        author.add(new InlineLabel(" said:"));
        add(author);

        add(MsoyUI.createLabel(comment.text, "Text"));

        add(MsoyUI.createLabel("on " + new Date(comment.posted), "Posted"));
    }
}
