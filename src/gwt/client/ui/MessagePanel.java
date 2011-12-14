//
// $Id$

package client.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.MessageUtil;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.ShellMessages;
import client.util.Link;

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

    public void setMessage (MemberCard poster, Date whenPosted, String text)
    {
        for (int ii = 0; getRowCount() > 0 && ii < 2; ii ++) {
            removeRow(0);
        }

        Widget box = getThumbBox(poster);
        boolean showRoleCaption = shouldShowRoleCaption();
        if (box != null) {
            if (showRoleCaption) {
                box = MsoyUI.createFlowPanel("roleBox", box, MsoyUI.createRoleLabel(poster.role));
            }
            setWidget(0, 0, box);
            if (text != null) {
                getFlexCellFormatter().setRowSpan(0, 0, 2);
            }
            getFlexCellFormatter().setStyleName(0, 0, "Photo");
            getFlexCellFormatter().addStyleName(0, 0, "BottomPad");
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        }

        FlowPanel info = new FlowPanel();
        info.setStyleName("MsgInfo");
        Widget icon = createIcon();
        if (icon != null) {
            icon.addStyleName("Icon");
            info.add(icon);
        }

        Widget name = Link.memberView(poster);
        name.addStyleName("Author");
        info.add(name);
        addAuthorInfo(info);

        // TODO: switch to "XX days/minutes ago"
        String when = DateUtil.formatDateTime(whenPosted);
        InlineLabel posted = new InlineLabel(when, false, true, false);
        posted.addStyleName("Posted");
        info.add(posted);
        addInfo(info);
        setWidget(0, 1, info);
        getFlexCellFormatter().setStyleName(0, 1, "LeftPad");

        Panel tools = getTools();
        if (tools != null) {
            setWidget(0, 2, tools);
            getFlexCellFormatter().setStyleName(0, 2, "Tools");
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        };

        ScrollPanel scroller = new ScrollPanel();
        scroller.addStyleName("Scroller");
        if (text != null) {
            if (textIsHTML()) {
                text = text.replaceAll(WHIRLED_REGEX, WHIRLED_REPLACE);
                text = text.replaceAll(
                    "href=\"\\s*[Jj][Aa][Vv][Aa][Ss][Cc][Rr][Ii][Pp][Tt]:.*\"", "href=\"#\"");
                text = MessageUtil.expandMessage(text);
                scroller.add(MsoyUI.createHTML(text, null));
            } else {
                scroller.add(MsoyUI.createRestrictedHTML(text));
            }
        }
        setWidget(1, 0, scroller);
        if (text != null) {
            getFlexCellFormatter().setStyleName(1, 0, "Text");
            getFlexCellFormatter().addStyleName(1, 0, "LeftPad");
            getFlexCellFormatter().addStyleName(1, 0, "BottomPad");
        }

        if (tools != null) {
            getFlexCellFormatter().setColSpan(1, 0, 2);
        }
    }

    /**
     * Return the ThumbBox to use for this poster. Override and return null for no photo.
     */
    protected ThumbBox getThumbBox (MemberCard poster)
    {
        return new ThumbBox(poster.photo, getThumbnailSize(),
                            Pages.PEOPLE, ""+poster.name.getId());
    }

    /**
     * Whether a caption explaining the role of the poster should be shown with the profile photo.
     * Subclasses can override to allow this.
     */
    protected boolean shouldShowRoleCaption ()
    {
        return false;
    }

    /**
     * If this message should have a toolbar floated to the right of the info section,
     * subclasses may return a Panel here.
     */
    protected Panel getTools ()
    {
        return null;
    }

    /**
     * Returns true if our message text is HTML, false if it is plain text.
     */
    protected boolean textIsHTML ()
    {
        return false;
    }

    /**
     * Creates the icon that will go to the left of the author name.
     */
    protected Widget createIcon ()
    {
        return null;
    }

    /**
     * Returns the size of thumbnail image to use next to our message.
     */
    protected int getThumbnailSize ()
    {
        return MediaDescSize.THUMBNAIL_SIZE;
    }

    /** Override to add widgets right after the author name. */
    protected void addAuthorInfo (FlowPanel info)
    {
    }

    /**
     * If a message wants to add anything to the right of the author and post time (like links for
     * editing or deleting), it should do so in this method.
     */
    protected void addInfo (FlowPanel info)
    {
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String WHIRLED_REGEX = "([^>\\\"])http://.*\\.whirled\\.com/#([^ <]+)";
    protected static final String WHIRLED_REPLACE = "$1<a href=\"#$2\">link</a>";
}
