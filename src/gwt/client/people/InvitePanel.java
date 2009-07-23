//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;

/**
 * Display a UI allowing users to send out invitations to Whirled.
 */
public abstract class InvitePanel extends FlowPanel
{
    /**
     * Creates a header with an image. We use this on invite panels as well as on the
     * ConfigureProfilePanel to provide a consistent UI experience during the four steps to
     * registration and friend spamming glory.
     */
    public static Widget makeHeader (String image, String text)
    {
        SmartTable header = new SmartTable("inviteHeader", 0, 10);
        header.setWidget(0, 0, new Image(image));
        header.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        header.setWidget(0, 2, MsoyUI.createHTML(text, "Title"));
        return header;
    }

    public InvitePanel ()
    {
        setStyleName("invitePanel");

        _buttons = new HorizontalPanel();
        _buttons.setWidth("100%");
        _buttons.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        _buttons.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
    }

    protected void addMethodButton (String label, final InviteMethodCreator creator)
    {
        _buttons.add(MsoyUI.createButton(MsoyUI.LONG_THICK, label, new ClickHandler() {
            public void onClick (ClickEvent event) {
                setMethod(creator.create()); //new SendURLPanel();
            }
        }));
    }

    protected void addMethodButtons ()
    {
        add(WidgetUtil.makeShim(10, 10));
        add(_buttons);
        add(WidgetUtil.makeShim(10, 10));
        _methodRow = getWidgetCount();
    }

    protected void setMethod (Widget panel)
    {
        while (getWidgetCount() > _methodRow) {
            remove(_methodRow);
        }
        if (panel != null) {
            add(panel);
        }
    }

    /** Allows various invite methods to be hooked up to click listeners. */
    protected static interface InviteMethodCreator
    {
        /** Creates the widget that will display this invite method. */
        Widget create ();
    }

    /**
     * Invite method consisting of a text area to copy a URL from.
     */
    protected static class IMPanel extends SmartTable
    {
        public IMPanel (String shareURL)
        {
            setStyleName("IM");
            setWidth("100%");
            setText(0, 0, _msgs.inviteIMCopy(), 1, "Bold");
            setText(1, 0, _msgs.inviteIMTip(), 1);
            TextBox link = new TextBox();
            link.setText(shareURL);
            link.setReadOnly(true);
            MsoyUI.selectAllOnFocus(link);
            setWidget(2, 0, link);
        }
    }

    /** The row where the invite method is. */
    protected int _methodRow;

    /** The buttons for the various invite methods. */
    protected HorizontalPanel _buttons;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
