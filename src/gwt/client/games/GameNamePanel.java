//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;

import client.shell.DynamicMessages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;

/**
 * Displays a game's name, genre, creator and description.
 */
public class GameNamePanel extends VerticalPanel
{
    public GameNamePanel (String name, byte genre, MemberName creator, String descrip)
    {
        setStyleName("gameName");
        add(MsoyUI.createLabel(name, "Name"));
        add(MsoyUI.createLabel(_dmsgs.getString("genre" + genre), "Genre"));
        add(WidgetUtil.makeShim(5, 5));
        add(new CreatorLabel(creator));
        add(MsoyUI.createLabel(descrip, "Descrip"));
    }

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
