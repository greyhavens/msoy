//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.Item;

/**
 * A base class for editors of game items.
 */
public abstract class GameItemEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _gitem = (GameItem)item;
    }


    @Override // from ItemEditor
    public void setGameId (int gameId)
    {
        _gitem.gameId = gameId;
    }

    protected GameItem _gitem;
}
