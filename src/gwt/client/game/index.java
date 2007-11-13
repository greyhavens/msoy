//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.client.GameServiceAsync;
import com.threerings.msoy.web.data.LaunchConfig;

import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.MsoyUI;

/**
 * Displays a page that allows a player to play a particular game. If it's single player the game
 * is shown, if it's multiplayer the lobby is first shown where the player can find opponents
 * against which to play.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (Args args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CGame.ident == null) {
            setContent(MsoyUI.createLabel(CGame.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        // if we have d-NNN then we want to see game detail
        String action = args.get(0, "");
        if (action.equals("d")) {
            GameDetailPanel panel;
            if (getContent() instanceof GameDetailPanel) {
                panel = (GameDetailPanel)getContent();
            } else {
                setContent(panel = new GameDetailPanel(this));
            }
            panel.setGame(args.get(1, 0), args.get(2, ""));

        } else if (action.equals("t")) {
            setContent(new TrophyCasePanel(this, args.get(1, 0)));

        } else {
            // TODO: display the arcade
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return GAME;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CGame.gamesvc = (GameServiceAsync)GWT.create(GameService.class);
        ((ServiceDefTarget)CGame.gamesvc).setServiceEntryPoint("/gamesvc");

        // load up our translation dictionaries
        CGame.msgs = (GameMessages)GWT.create(GameMessages.class);
    }
}
