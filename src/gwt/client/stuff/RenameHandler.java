//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.ShellMessages;
import client.util.BorderedDialog;
import client.util.ClickCallback;

public class RenameHandler extends ClickCallback<String>
{
    public RenameHandler (SourcesClickEvents trigger, Item item, InventoryModels models)
    {
        super(trigger, "");

        _item = item;
        _models = models;
        _name = new TextBox();
        _name.setMaxLength(Item.MAX_NAME_LENGTH);
        _name.setVisibleLength(Item.MAX_NAME_LENGTH);
        _name.setText(_item.name);
    }

    // from ClickCallback
    public boolean callService () {
        CStuff.itemsvc.renameClone(CStuff.ident, _item.getIdent(), _name.getText(), this);
        return true;
    }

    // from ClickCallback
    public boolean gotResult (String result) {
        _item.name = result;
        _models.updateItem(_item);
        // just force a reload of the detail page
        Application.replace(Page.STUFF, Args.compose(new String[] {
            "d", "" + _item.getType(), "" + _item.itemId, _item.name.replaceAll(" ", "-") }));
        return true;
    }

    // from ClickCallback
    protected void displayPopup ()
    {
        new BorderedDialog(false) {
            {
                setHeaderTitle(CStuff.msgs.renameTitle());

                VerticalPanel content = new VerticalPanel();
                content.setWidth("300px");
                content.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

                Label label = new Label(CStuff.msgs.renameTip());
                label.setStyleName("Header");
                content.add(label);

                content.add(_name);

                HorizontalPanel buts = new HorizontalPanel();
                buts.setSpacing(10);

                Button noButton = new Button(_cmsgs.cancel());
                final Button revertButton = new Button(CStuff.msgs.renameRevert());
                final Button yesButton = new Button(_cmsgs.change());
                ClickListener listener = new ClickListener() {
                    public void onClick (Widget sender) {
                        if (sender == revertButton) {
                            _name.setText("");
                            takeAction(true);
                        } else if (sender == yesButton) {
                            takeAction(true);
                        }
                        hide();
                    }
                };
                noButton.addClickListener(listener);
                revertButton.addClickListener(listener);
                yesButton.addClickListener(listener);
                buts.add(noButton);
                buts.add(revertButton);
                buts.add(yesButton);

                content.add(buts);

                setContents(content);
                show();
            }

            protected void onClosed (boolean autoClosed) {
                setEnabled(true);
            }
        };
    }

    protected Item _item;
    protected InventoryModels _models;
    protected TextBox _name;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
