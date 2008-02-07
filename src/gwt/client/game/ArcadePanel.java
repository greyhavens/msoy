//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Main game display.
 */
public class ArcadePanel extends VerticalPanel
{
    public ArcadePanel ()
    {
        setStyleName("arcade");
        setSpacing(5);

        HorizontalPanel row = new HorizontalPanel();
        row.add(new WhatIsGamesPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new FeaturedGamePanel());
        add(row);

        row = new HorizontalPanel();
        row.add(new CategoriesPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new GameCategoryPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new GameCategoryPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new GameCategoryPanel());
        add(row);
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
        public FeaturedGamePanel ()
        {
            setStyleName("FeaturedGame");
        }
    }

    protected static class CategoriesPanel extends FlexTable
    {
        public CategoriesPanel ()
        {
            setStyleName("Categories");
        }
    }

    protected static class GameCategoryPanel extends FlexTable
    {
        public GameCategoryPanel ()
        {
            setStyleName("GameCategory");
        }
    }
}
