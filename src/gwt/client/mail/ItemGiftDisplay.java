//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.person.gwt.ItemGiftPayload;
import com.threerings.msoy.person.gwt.MailService;
import com.threerings.msoy.person.gwt.MailServiceAsync;

import client.item.ItemThumbnail;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays item gift mail payloads.
 */
public class ItemGiftDisplay extends MailPayloadDisplay
{
    @Override
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget(true);
    }

    @Override
    public Widget widgetForSender ()
    {
        return new DisplayWidget(false);
    }

    @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        _giftPayload = (ItemGiftPayload) _message.payload;
    }

    protected class DisplayWidget extends FlowPanel
    {
        public DisplayWidget (boolean enabled)
        {
            super();
            _enabled = enabled;
            buildUI();
        }

        protected void buildUI ()
        {
            clear();

            if (_giftPayload.item == null) {
                add(MsoyUI.createLabel(CMail.msgs.giftGone(), null));
                return;
            }

            if (_enabled) {
                add(MsoyUI.createLabel(CMail.msgs.giftItem(), null));
            }

            _itemsvc.loadItem(CMail.ident, _giftPayload.item, new MsoyCallback<Item>() {
                public void onSuccess (Item result) {
                    add(new ItemThumbnail(result, _enabled ? new ClickListener() {
                        public void onClick (Widget sender) {
                            unwrapItem();
                        }
                    } : null));
                }
            });
        }

        protected void unwrapItem ()
        {
            if (_giftPayload.item == null) {
                // this happens if the user clicks the thumbnail a second time after the unwrapping
                // suceeds, but before the payload state is updated on the server; just swallow the
                // click
                return;
            }
            _itemsvc.wrapItem(CMail.ident, _giftPayload.item, false, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    // the item is unwrapped, just update the payload
                    _giftPayload.item = null;
                    updateState(_giftPayload, new MsoyCallback<Void>() {
                        public void onSuccess (Void result) {
                            // all went well: rebuild the view
                            buildUI();
                        }
                    });
                }
            });
        };

        protected boolean _enabled;
    }

    protected ItemGiftPayload _giftPayload;

    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
