//
// $Id$

package client.item;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.web.client.WebContext;

/**
 * Defines the base item detail panel from which we derive an inventory item detail and a catalog
 * item detail.
 */
public class BaseItemDetailPanel extends VerticalPanel
{
    protected BaseItemDetailPanel (WebContext ctx, Item item)
    {
        _ctx = ctx;
        _item = item;

        // create our user interface
        HorizontalPanel title = new HorizontalPanel();
        title.add(_name = new Label(item.getDescription()));
        title.add(_creator = new Label(""));
        // TODO: add a close box
        add(title);

        HorizontalPanel middle = new HorizontalPanel();
        middle.add(ItemUtil.createMediaView(item.getPreviewMedia(), false));
        middle.add(_details = new VerticalPanel());
        createDetailsInterface(_details);
        add(middle);

        // TODO: add tag stuff

        // load up the item details
        _ctx.itemsvc.loadItemDetail(_ctx.creds, _item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                gotDetail(_detail = (ItemDetail)result);
            }
            public void onFailure (Throwable caught) {
                // TODO: translate, unhack
                _description.setText("Failed to load item details: " + caught);
            }
        });
    }

    protected void createDetailsInterface (VerticalPanel details)
    {
        details.add(_description = new Label("..."));
    }

    protected void gotDetail (ItemDetail detail)
    {
        _creator.setText("by " + detail.creator.toString());
    }

    protected WebContext _ctx;
    protected Item _item;
    protected ItemDetail _detail;

    protected VerticalPanel _details;
    protected Label _name, _creator, _description;
}
