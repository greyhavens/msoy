//
// $Id$

package client.util;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
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

import client.editem.EditorHost;
import client.editem.ItemEditor;
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
        contents.add(MsoyUI.createLabel(CShell.cmsgs.pickImage(), "Title"));
        contents.add(new PhotoGrid(images));
        setWidget(contents);
    }

    protected void photoSelected (Photo photo)
    {
        _callback.onSuccess(photo);
        hide();
    }

    protected class PhotoGrid extends PagedGrid
        implements EditorHost
    {
        public PhotoGrid (List images) {
            super(3, 5, NAV_ON_BOTTOM);
            setCellAlignment(HasAlignment.ALIGN_CENTER, HasAlignment.ALIGN_MIDDLE);
            setModel(new SimpleDataModel(images), 0);
        }

        // from interface EditorHost
        public void editComplete (Item item) {
            CShell.log("Edit complete " + item);
            if (item != null) {
                ((SimpleDataModel)_model).addItem(0, item);
                CShell.log("Added to model");
                displayPage(0, true);
                CShell.log("Updated page");
            }
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item) {
            final Photo photo = (Photo)item;
            Widget image = MediaUtil.createMediaView(
                photo.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
            ((Image)image).addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    photoSelected(photo);
                }
            });
            image.setStyleName("Photo");
            return image;
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage () {
            return CShell.cmsgs.haveNoImages();
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items) {
            return true;
        }

        // @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls) {
            super.addCustomControls(controls);
            Button upload = new Button(CShell.cmsgs.uploadImage(), new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(Item.PHOTO, PhotoGrid.this);
                    editor.setItem(editor.createBlankItem());
                    editor.show();
                }
            });
            controls.setWidget(0, 0, upload);
        }
    }

    protected AsyncCallback _callback;
}
