//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.shell.Args;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a dialog that handles renaming of a cloned item.
 */
public class RenameHandler extends ClickCallback<String>
{
    public RenameHandler (SourcesClickEvents trigger, Item item, ItemDataModel detailListener)
    {
        super(trigger, "");

        _item = item;
        _listener = detailListener;
        _name = new TextBox();
        _name.setMaxLength(Item.MAX_NAME_LENGTH);
        _name.setVisibleLength(Item.MAX_NAME_LENGTH);
        _name.setText(_item.name);
    }

    // from ClickCallback
    public boolean callService () {
        _stuffsvc.renameClone(_item.getIdent(), _name.getText(), this);
        return true;
    }

    // from ClickCallback
    public boolean gotResult (String result) {
        _item.name = result;
        _listener.itemUpdated(_item);
        // just force a reload of the detail page
        Link.replace(Pages.STUFF, Args.compose(new String[] {
                    "d", "" + _item.getType(), "" + _item.itemId,
                    _item.name.replaceAll(" ", "-") }));
        return true;
    }

    // from ClickCallback
    protected void displayPopup ()
    {
        BorderedDialog dialog = new BorderedDialog(false) {
            protected void onClosed (boolean autoClosed) {
                setEnabled(true);
            }
        };

        dialog.setHeaderTitle(_msgs.renameTitle());

        SmartTable content = new SmartTable(0, 10);
        content.setWidth("300px");
        content.setText(0, 0, _msgs.renameTip());
        content.setWidget(1, 0, _name);
        dialog.setContents(content);

        dialog.addButton(new Button(_cmsgs.cancel(), dialog.onCancel()));
        dialog.addButton(new Button(_msgs.renameRevert(), dialog.onAction(new Command() {
            public void execute () {
                _name.setText("");
                takeAction(true);
            }
        })));
        dialog.addButton(new Button(_cmsgs.change(), dialog.onAction(new Command() {
            public void execute () {
                takeAction(true);
            }
        })));

        dialog.show();
    }

    protected Item _item;
    protected ItemDataModel _listener;
    protected TextBox _name;

    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
