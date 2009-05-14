//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.ServiceUtil;
import client.shell.DynamicLookup;

/**
 * Displays a list of game subitems (trophy source, level pack, etc.).
 */
public class GameItemEditorPanel extends SmartTable
{
    public GameItemEditorPanel (int gameId, byte itemType)
    {
        super("gameItemEditor", 0, 10);
        _itemType = itemType;

        setText(0, 0, "Loading...");

        _gamesvc.loadGameItems(gameId, itemType, new InfoCallback<List<Item>>() {
            public void onSuccess (List<Item> items) {
                init(items);
            }
        });
    }

    protected void init (List<Item> items)
    {
        if (items.size() == 0) {
            setText(0, 0, "Nada!");
            return;
        }

        int col = 0;
        setText(0, col++, ""); // clear out Loading...
        setText(0, col++, "Name", 1, "Header");
        if (items.get(0) instanceof SubItem) {
            setText(0, col++, "Ident", 1, "Header");
        }
        if (items.get(0) instanceof TrophySource) {
            setText(0, col++, "Secret", 1, "Header");
        } else if (items.get(0) instanceof LevelPack) {
            setText(0, col++, "Premium", 1, "Header");
        } else if (items.get(0) instanceof Prize) {
            setText(0, col++, "Prize", 1, "Header");
        }

        int row = 1;
        for (Item item : items) {
            String dargs = Args.compose("d", item.getType(), item.itemId);
            String eargs = Args.compose("e", item.getType(), item.itemId);
            col = 0;
            setWidget(row, col++, MediaUtil.createMediaView(
                          item.getThumbnailMedia(), MediaDesc.QUARTER_THUMBNAIL_SIZE), 1, null);
            setWidget(row, col++, Link.create(item.name, Pages.STUFF, dargs), 1, null);
            if (item instanceof SubItem) {
                setText(row, col++, ((SubItem)item).ident);
            }
            if (item instanceof TrophySource) {
                setText(row, col++, ((TrophySource)item).secret ? "secret" : "");
            } else if (item instanceof LevelPack) {
                setText(row, col++, ((LevelPack)item).premium ? "premium" : "free");
            } else if (item instanceof Prize) {
                Prize prize = (Prize)item;
                String pargs = Args.compose("l", prize.targetType, prize.targetCatalogId);
                setWidget(row, col++, Link.create("[view]", Pages.SHOP, pargs));
            }
            setWidget(row, col++, Link.create("[edit]", Pages.STUFF, eargs));
            row++;
        }
    }

    protected byte _itemType;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
