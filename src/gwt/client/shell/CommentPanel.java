//
// $Id$

package client.shell;

import java.util.Date;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;

/**
 * Displays a single comment.
 */
public class CommentPanel extends FlexTable
{
    public CommentPanel (final CommentsPanel parent, final Comment comment)
    {
        setStyleName("commentPanel");
        setCellPadding(0);
        setCellSpacing(0);

        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.PROFILE, "" + comment.commentor.getMemberId());
            }
        };
        Widget photo = MediaUtil.createMediaView(comment.photo, MediaDesc.THUMBNAIL_SIZE);
        if (photo instanceof Image) {
            ((Image) photo).addClickListener(onClick);
            photo.setStyleName("actionLabel");
        }
        setWidget(0, 0, photo);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Photo");
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);

        FlowPanel info = new FlowPanel();
        InlineLabel author = new InlineLabel(comment.commentor.toString());
        author.addClickListener(onClick);
        author.addStyleName("Author");
        author.addStyleName("actionLabel");
        info.add(author);

        // TODO: switch to "XX days/minutes ago"
        String when = CShell.cmsgs.postedOn(_pfmt.format(new Date(comment.posted)));
        InlineLabel posted = new InlineLabel(when, false, true, false);
        posted.addStyleName("Posted");
        info.add(posted);

        if (CShell.getMemberId() == comment.commentor.getMemberId() || CShell.isSupport()) {
            InlineLabel delete = new InlineLabel(CShell.cmsgs.deletePost(), false, true, false);
            delete.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    parent.deleteComment(CommentPanel.this, comment);
                    setText(2, 1, CShell.cmsgs.deletingPost());
                }
            });
            delete.addStyleName("Posted");
            delete.addStyleName("actionLabel");
            info.add(delete);
        }
        setWidget(0, 1, info);

        setText(1, 0, comment.text);
        getFlexCellFormatter().setStyleName(1, 0, "Text");
    }

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy h:mm:ss aa");
}
