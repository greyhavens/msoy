//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.LaunchConfig;

/**
 * Displays the client interface for a particular game.
 */
public class GamePanel extends VerticalPanel
{
    public GamePanel (WebContext ctx, LaunchConfig config)
    {
        add(new Label(config.name));
        add(new Label(config.gameMediaURL));
        add(new Label(config.server));
        add(new Label("" + config.port));
    }
}
