//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.Launcher;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Launcher} digital items.
 */
public class LauncherEditor extends GameItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Launcher)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Launcher();
    }

    protected Launcher _game;
}
