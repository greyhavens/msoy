//
// $Id$

package client.shell;

import java.util.Date;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import client.util.Link;
import client.util.MsoyUI;
import client.util.ThumbBox;

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
        ClickListener onClick = Link.createListener(
            Page.PEOPLE, "" + poster.name.getMemberId());
        setWidget(0, 0, new ThumbBox(poster.photo, getThumbnailSize(), onClick));
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Photo");
        getFlexCellFormatter().addStyleName(0, 0, "BottomPad");
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        FlowPanel info = new FlowPanel();
        info.setStyleName("Info");
        String iconPath = getIconPath();
        if (iconPath != null) {
            Image icon = new Image(iconPath);
            icon.addStyleName("Icon");
            info.add(icon);
        }

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

        ScrollPanel scroller = new ScrollPanel();
        scroller.addStyleName("Scroller");
        if (textIsHTML()) {
            text = text.replaceAll(WHIRLED_REGEX, WHIRLED_REPLACE);
            text = text.replaceAll("href=\"\\s*[Jj][Aa][Vv][Aa][Ss][Cc][Rr][Ii][Pp][Tt]:.*\"", "href=\"#\"");
            scroller.add(new HTML(text));
        } else {
            scroller.add(MsoyUI.createRestrictedHTML(text));
        }
        setWidget(1, 0, scroller);

        getFlexCellFormatter().setStyleName(1, 0, "Text");
        getFlexCellFormatter().addStyleName(1, 0, "LeftPad");
        getFlexCellFormatter().addStyleName(1, 0, "BottomPad");
    }

    /**
     * Returns true if our message text is HTML, false if it is plain text.
     */
    protected boolean textIsHTML ()
    {
        return false;
    }

    /**
     * If a message wants to display an icon to the left of the poster's name it can return the
     * path to said image here and the icon will automatically be inserted.
     */
    protected String getIconPath ()
    {
        return null;
    }

    /**
     * Returns the size of thumbnail image to use next to our message.
     */
    protected int getThumbnailSize ()
    {
        return MediaDesc.THUMBNAIL_SIZE;
    }

    /**
     * If a message wants to add anything to the right of the author and post time (like links for
     * editing or deleting), it should do so in this method.
     */
    protected void addInfo (FlowPanel info)
    {
    }

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy h:mm aa");

    protected static final String WHIRLED_REGEX = "([^>\\\"])http://.*\\.whirled\\.com/#([^ <]+)";
    protected static final String WHIRLED_REPLACE = "$1<a href=\"#$2\">link</a>";
}
