//
// $Id$

package client.util;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.shell.CShell;
import client.util.BorderedPopup;
import client.util.MediaUtil;

/**
 * Allows a member to select an image from their inventory. In the future, will support fancy
 * things like immediately uploading an image for use instead of getting one from their inventory,
 * and possibly doing a Google images or Flickr search.
 */
public class ImageChooserPopup extends BorderedPopup
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

            PagedGrid grid = new PagedGrid(3, 5, PagedGrid.NAV_ON_BOTTOM) {
                protected Widget createWidget (Object item) {
                    final Photo photo = (Photo)item;
                    Widget image = MediaUtil.createMediaView(
                        photo.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
                    ((Image)image).addClickListener(new ClickListener() {
                        public void onClick (Widget sender) {
                            _callback.onSuccess(photo);
                            ImageChooserPopup.this.hide();
                        }
                    });
                    image.setStyleName("Photo");
                    return image;
                }
                protected String getEmptyMessage () {
                    return ""; // not used
                }
            };
            grid.setCellAlignment(HasAlignment.ALIGN_CENTER, HasAlignment.ALIGN_MIDDLE);
            grid.setModel(new SimpleDataModel(images), 0);
            contents.add(grid);

        } else {
            contents.add(MsoyUI.createLabel(CShell.cmsgs.haveNoImages(), "Title"));
        }
    }

    protected AsyncCallback _callback;
}
