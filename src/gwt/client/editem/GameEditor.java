//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Game();
    }

    protected Game _game;
}
