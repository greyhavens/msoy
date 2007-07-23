//
// $Id$

package client.inventory;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;

public class DoListItemPopup extends BorderedDialog
{
    public DoListItemPopup (Item item)
    {
        super(false, true);
        addStyleName("doListItem");
        _item = item;

        _status = new Label("");
        _status.addStyleName("Status");

        String title = CInventory.msgs.doListHdrTop(Item.getTypeName(item.getType()));
        _header.add(createTitleLabel(title, null));
        _content.add(MsoyUI.createLabel(CInventory.msgs.doListBlurb(), "Blurb"));

        _content.add(MsoyUI.createLabel(CInventory.msgs.doListDescripHeader(), "Header"));
        _content.add(MsoyUI.createLabel(CInventory.msgs.doListNeedsDescrip(), "Blurb"));
        _content.add(_description = new TextArea());
        _description.setText(item.description);
        _description.setCharacterWidth(40);
        _description.setVisibleLines(3);
        _description.addKeyboardListener(_valdescrip);

        _content.add(MsoyUI.createLabel(CInventory.msgs.doListRarityHeader(), "Header"));
        _content.add(MsoyUI.createLabel(CInventory.msgs.doListRarityBlurb(), "Blurb"));

        FlowPanel flow = new FlowPanel();
        flow.addStyleName("RarityRow");
        flow.add(new InlineLabel(CInventory.msgs.doListHdrRarity(), false, false, true));

        final ListBox rarityBox = new ListBox();
        rarityBox.addStyleName("RarityBox");

        rarityBox.addItem(CInventory.msgs.doListRarityPlentiful());
        rarityBox.addItem(CInventory.msgs.doListRarityCommon());
        rarityBox.addItem(CInventory.msgs.doListRarityNormal());
        rarityBox.addItem(CInventory.msgs.dolistRarityUncommon());
        rarityBox.addItem(CInventory.msgs.doListRarityRare());

        rarityBox.setSelectedIndex(2);
        // TEMP: List pricing is currently disabled
        /*rarityBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                ListBox box = (ListBox) sender;
                _priceBox.setText(String.valueOf(_prices[box.getSelectedIndex()]));
            }
        });*/
        flow.add(rarityBox);
        _content.add(flow);

        // TEMP: List pricing is currently disabled
        /*flow = new FlowPanel();
        flow.addStyleName("PriceRow");
        flow.add(new InlineLabel(CInventory.msgs.doListHdrPrice(), false, false, true));

        flow.add(new Image("/images/header/symbol_flow.png"));
        _priceBox = new Label(String.valueOf(_prices[2]));
        _priceBox.addStyleName("Price");
        flow.add(_priceBox);
        _content.add(flow);*/

        _content.add(_status);

        _footer.add(new Button(CInventory.msgs.doListBtnCancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));

        _footer.add(_listIt = new Button(CInventory.msgs.doListBtnListIt()));
        new ClickCallback(_listIt) {
            public boolean callService () {
                int rarity = _rarities[Math.max(0, rarityBox.getSelectedIndex())];
                CInventory.catalogsvc.listItem(
                    CInventory.ident, _item.getIdent(), _description.getText(), rarity, true, this);
                return true;
            }
            public boolean gotResult (Object result) {
                // TODO: enhance dialog to link to catalog page to see item
                MsoyUI.info(CInventory.msgs.msgItemListed());
                hide();
                return false;
            }
        };

        validateDescription();
    }

    protected void validateDescription ()
    {
        _listIt.setEnabled(_description.getText().trim().length() > 0);
    }

    // @Override
    protected Widget createContents ()
    {
        _content = new VerticalPanel();
        _content.addStyleName("Content");
        _content.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        return _content;
    }

    protected KeyboardListener _valdescrip = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validateDescription();
                }
            });
        }
    };

    protected Label _status;
    protected Label _priceBox;
    protected VerticalPanel _content;
    protected TextArea _description;
    protected Button _listIt;
    protected Item _item;

    protected static final int[] _rarities = new int[] {
        CatalogListing.RARITY_PLENTIFUL, CatalogListing.RARITY_COMMON,
        CatalogListing.RARITY_NORMAL, CatalogListing.RARITY_UNCOMMON,
        CatalogListing.RARITY_RARE
    };
    
    protected static final int[] _prices = new int[] { 100, 200, 300, 400, 500 };
}
