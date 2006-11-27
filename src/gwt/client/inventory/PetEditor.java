//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Pet} digital items.
 */
public class PetEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pet = (Pet)item;
        _descrip.setText((_pet.description == null) ? "" :
                         _pet.description);
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        super.createEditorInterface();

        addRow("Description", _descrip = new TextBox());
        bind(_descrip, new Binder() {
            public void textUpdated (String text) {
                _pet.description = text;
            }
        });
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Pet();
    }

    protected Pet _pet;
    protected TextBox _descrip;
}
