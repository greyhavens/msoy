//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

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
        setStyleName("fbarcade");
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
        add(WidgetUtil.createFlashContainer(info.tag, info.swfURL, info.width, info.height, null));
    }

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
