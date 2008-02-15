//
// $Id$

package client.util;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.shell.CShell;
import client.shell.Frame;

/**
 * Allows a member to select an image from their inventory. In the future, will support fancy
 * things like immediately uploading an image for use instead of getting one from their inventory,
 * and possibly doing a Google images or Flickr search.
 */
public class ImageChooserPopup extends VerticalPanel
{
    /**
     * Displays an image chooser which will select either the main or thumbnail image of a photo
     * from the user's inventory, or allow them to upload an image directly.
     */
    public static void displayImageChooser (final boolean thumbnail, final AsyncCallback callback)
    {
        CShell.membersvc.loadInventory(CShell.ident, Item.PHOTO, 0, new AsyncCallback() {
            public void onSuccess (Object result) {
                Frame.showDialog(CShell.cmsgs.pickImage(),
                                 new ImageChooserPopup((List)result, thumbnail, callback));
            }
            public void onFailure (Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    protected ImageChooserPopup (List images, boolean thumbnail, AsyncCallback callback)
    {
        _callback = callback;
        _thumbnail = thumbnail;
        setStyleName("imageChooser");
        add(new PhotoGrid(images));
    }

    protected class PhotoGrid extends PagedGrid
        implements EditorHost
    {
        public PhotoGrid (List images) {
            super(2, 7, NAV_ON_BOTTOM);
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
            Photo photo = (Photo)item;
            if (photo == null) {
                return null;
            }
            final MediaDesc media = _thumbnail ? photo.photoMedia : photo.getThumbnailMedia();
            Widget image = MediaUtil.createMediaView(
                photo.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
                    public void onClick (Widget sender) {
                        _callback.onSuccess(media);
                        Frame.clearDialog(ImageChooserPopup.this);
                    }
                });
            image.addStyleName("Photo");
            return image;
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage () {
            return CShell.cmsgs.haveNoImages();
        }

        // @Override // from PagedGrid
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, int limit) {
            super.formatCell(formatter, row, col, limit);
            formatter.setWidth(row, col, MediaDesc.THUMBNAIL_WIDTH+"px");
            formatter.setHeight(row, col, MediaDesc.THUMBNAIL_HEIGHT+"px");
        }

        // @Override // from PagedGrid
        protected boolean padToFullPage () {
            return true;
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items) {
            return true;
        }

// TODO
//         // @Override // from PagedGrid
//         protected void addCustomControls (FlexTable controls) {
//             super.addCustomControls(controls);
//             Button upload = new Button(CShell.cmsgs.uploadImage(), new ClickListener() {
//                 public void onClick (Widget sender) {
//                     ItemEditor editor = ItemEditor.createItemEditor(Item.PHOTO, PhotoGrid.this);
//                     editor.setItem(editor.createBlankItem());
//                     editor.show();
//                 }
//             });
//             controls.setWidget(0, 0, upload);
//         }
    }

    protected boolean _thumbnail;
    protected AsyncCallback _callback;
}
