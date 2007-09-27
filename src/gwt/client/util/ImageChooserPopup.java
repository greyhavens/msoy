//
// $Id$

package client.util;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;

import client.shell.CShell;
import client.util.BorderedPopup;

/**
 * Allows a member to select an image from their inventory. In the future, will support fancy
 * things like immediately uploading an image for use instead of getting one from their inventory,
 * and possibly doing a Google images or Flickr search.
 */
public class ImageChooserPopup extends BorderedPopup
    implements ClickListener
{
    public static void displayImageChooser (final AsyncCallback callback)
    {
        CShell.membersvc.loadInventory(CShell.ident, Item.PHOTO, 0, new AsyncCallback() {
            public void onSuccess (Object result) {
                new ImageChooserPopup((List)result, callback).show();
            }
            public void onFailure (Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    /**
     * Creates an image chooser popup with the supplied list of Photo items and a callback to be
     * informed when one is selected.
     */
    public ImageChooserPopup (List images, AsyncCallback callback)
    {
        super(true);
        _callback = callback;

        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("imageChooser");
        setWidget(contents);

        // iterate over all our photos and fill the popup panel
        if (images.size() > 0) {
            contents.add(MsoyUI.createLabel(CShell.cmsgs.pickImage(), "Title"));

            HorizontalPanel itemPanel = new HorizontalPanel();
            ScrollPanel chooser = new ScrollPanel(itemPanel);
            contents.add(chooser);

            for (Iterator iter = images.iterator(); iter.hasNext(); ) {
                Image image = new PhotoThumbnailImage(((Photo) iter.next()));
                image.addClickListener(this);
                itemPanel.add(image);
            }

        } else {
            contents.add(MsoyUI.createLabel(CShell.cmsgs.haveNoImages(), "Title"));
        }
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        _callback.onSuccess(((PhotoThumbnailImage)sender).photo);
        hide();
    }

    /**
     * A tiny helper class that carries a Photo in a Widget.
     */
    protected static class PhotoThumbnailImage extends Image
    {
        public Photo photo;

        protected PhotoThumbnailImage (Photo photo) {
            super(photo.getThumbnailPath());
            this.photo = photo;
        }
    }

    protected AsyncCallback _callback;
}
