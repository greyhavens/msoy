//
// $Id$

package client.item;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.BorderedDialog;
import client.util.MediaUtil;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public class BaseItemDetailPopup extends BorderedDialog
{
    protected BaseItemDetailPopup (Item item)
    {
        super(true);
        _item = item;

        // create our user interface
        _header.add(_name = new Label(item.name));
        _name.setStyleName("itemDetailName");
        _header.add(_creator = new Label(""));
        _creator.setStyleName("itemDetailCreator");
        // this is a goddamned hack, but GWT doesn't support valign=baseline, dooh!
        DOM.setStyleAttribute(DOM.getParent(_name.getElement()), "verticalAlign", "baseline");
        DOM.setStyleAttribute(DOM.getParent(_creator.getElement()), "verticalAlign", "baseline");

        // configure our item preview
        ((FlexTable)_contents).setWidget(0, 0, createPreview(item));

        // allow derived classes to add their own nefarious bits
        createInterface(_details, _controls);

        // add our tag business at the bottom
        _footer.add(new TagDetailPanel(item));

        // load up the item details
        CItem.itemsvc.loadItemDetail(CItem.creds, _item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                gotDetail(_detail = (ItemDetail)result);
                recenter();
            }
            public void onFailure (Throwable caught) {
                _description.setText(CItem.serverError(caught));
            }
        });
    }

    protected Widget createContents ()
    {
        FlexTable middle = new FlexTable();
        middle.setStyleName("itemDetailContent");

        // a place for the item's preview visualization
        middle.getFlexCellFormatter().setStyleName(0, 0, "itemDetailPreview");
        middle.getFlexCellFormatter().setRowSpan(0, 0, 2);

        // a place for details
        middle.setWidget(0, 1, _details = new VerticalPanel());
        middle.getFlexCellFormatter().setVerticalAlignment(0, 1, VerticalPanel.ALIGN_TOP);
        _details.setStyleName("itemDetailDetails");

        // a place for controls
        middle.setWidget(1, 0, _controls = new VerticalPanel());
        middle.getFlexCellFormatter().setVerticalAlignment(1, 0, VerticalPanel.ALIGN_BOTTOM);
        _controls.setStyleName("itemDetailControls");
        return middle;
    }

    protected Widget createPreview (Item item)
    {
        return MediaUtil.createMediaView(item.getPreviewMedia(), MediaDesc.PREVIEW_SIZE);
    }

    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        details.add(_description = new Label(ItemUtil.getDescription(_item)));
    }

    protected void gotDetail (ItemDetail detail)
    {
        _creator.setText(CItem.imsgs.detailBy(detail.creator.toString()));
        if (_item.isRatable()) {
            _details.add(new ItemRating(detail.item, detail.memberRating));
        }
    }

    protected Item _item;
    protected ItemDetail _detail;

    protected VerticalPanel _details, _controls;
    protected Label _name, _creator, _description;
}
