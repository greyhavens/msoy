//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;

import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Main game display.
 */
public class ArcadePanel extends VerticalPanel
{
    public ArcadePanel ()
    {
        setStyleName("arcade");
        setSpacing(5);

        CGame.gamesvc.loadArcadeData(CGame.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((ArcadeData)result);
            }
        });
    }

    protected void init (ArcadeData data)
    {
        HorizontalPanel row = new HorizontalPanel();
        row.add(new WhatIsGamesPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new FeaturedGamePanel(data.featuredGame));
        add(row);

        for (int ii = 0; ii < data.genres.size(); ii++) {
            if (ii % 3 == 0) {
                row = new HorizontalPanel();
                add(row);
            }
            row.add(new GameGenrePanel((ArcadeData.Genre)data.genres.get(ii)));
            if (ii % 3 != 2) {
                row.add(WidgetUtil.makeShim(5, 5));
            }
        }
    }

    protected static class WhatIsGamesPanel extends FlexTable
    {
        public WhatIsGamesPanel ()
        {
            setStyleName("WhatIsGames");
        }
    }

    protected static class MyGamesPanel extends FlexTable
    {
        public MyGamesPanel ()
        {
            setStyleName("MyGames");
        }
    }

    protected static class FeaturedGamePanel extends FlexTable
    {
        public FeaturedGamePanel (FeaturedGameInfo game)
        {
            setStyleName("FeaturedGame");

            setText(0, 0, game.name);
            getFlexCellFormatter().setColSpan(0, 0, 2);

            ClickListener onClick = new ClickListener() {
                public void onClick (Widget widget) {
                    // TODO
                }
            };

            MediaDesc shot = (game.screenshot == null) ?
                Item.getDefaultThumbnailMediaFor(Item.GAME) : game.screenshot;
            setWidget(1, 0, MediaUtil.createMediaView(
                          shot, Game.SHOT_WIDTH, Game.SHOT_HEIGHT, onClick));
            getFlexCellFormatter().setRowSpan(1, 0, 2);

            setText(1, 1, "Players online: " + game.playersOnline);

            setText(2, 0, game.description);
        }
    }

    protected static class GameGenrePanel extends FlexTable
    {
        public GameGenrePanel (ArcadeData.Genre genre)
        {
            setStyleName("GameGenre");
        }
    }
}
