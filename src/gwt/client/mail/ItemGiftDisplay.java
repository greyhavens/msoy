//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.person.data.ItemGiftPayload;

import client.util.ItemThumbnail;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays item gift mail payloads.
 */
public class ItemGiftDisplay extends MailPayloadDisplay
{
    // @Override
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget(true);
    }

    // @Override
    public Widget widgetForSender ()
    {
        return new DisplayWidget(false);
    }

    // @Override // from MailPayloadDisplay
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

            add(MsoyUI.createLabel(CMail.msgs.giftItem(), null));

            CMail.itemsvc.loadItem(CMail.ident, _giftPayload.item, new MsoyCallback() {
                public void onSuccess (Object result) {
                    add(new ItemThumbnail((Item) result, _enabled ? new ClickListener() {
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
            CMail.itemsvc.wrapItem(CMail.ident, _giftPayload.item, false, new MsoyCallback() {
                public void onSuccess (Object result) {
                    // the item is unwrapped, just update the payload
                    _giftPayload.item = null;
                    updateState(_giftPayload, new MsoyCallback() {
                        public void onSuccess (Object result) {
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
}
