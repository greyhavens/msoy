//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.MochiGameInfo;

import client.ui.MsoyUI;
import client.util.PageCallback;

/**
 * Play a mochi game.
 */
public class MochiGamePanel extends FlowPanel
{
    public MochiGamePanel (String mochiTag)
    {
        setStyleName("mochiPanel");
        add(MsoyUI.createNowLoading());
        _gamesvc.getMochiGame(mochiTag, new PageCallback<MochiGameInfo>(this) {
            public void onSuccess (MochiGameInfo info) {
                init(info);
            }
        });
    }

    protected void init (MochiGameInfo info)
    {
        clear();
        Widget game = WidgetUtil.createFlashContainer(
            info.tag, info.swfURL, info.width, info.height, null);
        game.addStyleName("Game");
        DOM.setStyleAttribute(game.getElement(), "width", info.width + "px");
        DOM.setStyleAttribute(game.getElement(), "height", info.height + "px");
        DOM.setStyleAttribute(game.getElement(), "marginLeft", "auto");
        DOM.setStyleAttribute(game.getElement(), "marginRight", "auto");
        add(game);
    }

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
