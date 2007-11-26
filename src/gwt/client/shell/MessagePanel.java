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

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import client.util.MediaUtil;

/**
 * Abstract display of a message posted by a member.
 */
public class MessagePanel extends FlexTable
{
    public MessagePanel ()
    {
        setStyleName("messagePanel");
        setCellPadding(0);
        setCellSpacing(0);
    }

    public void setMessage (final MemberCard poster, Date whenPosted, String text)
    {
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.PROFILE, "" + poster.name.getMemberId());
            }
        };
        Widget photo = MediaUtil.createMediaView(poster.photo, MediaDesc.THUMBNAIL_SIZE);
        if (photo instanceof Image) {
            ((Image) photo).addClickListener(onClick);
            photo.setStyleName("actionLabel");
        }
        setWidget(0, 0, photo);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Photo");
        getFlexCellFormatter().addStyleName(0, 0, "BottomPad");
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        FlowPanel info = new FlowPanel();
        InlineLabel author = new InlineLabel(poster.name.toString());
        author.addClickListener(onClick);
        author.addStyleName("Author");
        author.addStyleName("actionLabel");
        info.add(author);

        // TODO: switch to "XX days/minutes ago"
        String when = CShell.cmsgs.postedOn(_pfmt.format(whenPosted));
        InlineLabel posted = new InlineLabel(when, false, true, false);
        posted.addStyleName("Posted");
        info.add(posted);
        addInfo(info);
        setWidget(0, 1, info);
        getFlexCellFormatter().setStyleName(0, 1, "LeftPad");

        if (textIsHTML()) {
            setHTML(1, 0, text);
        } else {
            setText(1, 0, text);
        }
        getFlexCellFormatter().setStyleName(1, 0, "Text");
        getFlexCellFormatter().addStyleName(1, 0, "LeftPad");
        getFlexCellFormatter().addStyleName(1, 0, "BottomPad");
    }

    protected boolean textIsHTML ()
    {
        return false;
    }

    protected void addInfo (FlowPanel info)
    {
    }

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy h:mm aa");
}
