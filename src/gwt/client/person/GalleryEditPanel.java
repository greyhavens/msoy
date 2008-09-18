//
// $Id$

package client.person;

import java.util.ArrayList;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 *
 * @author mjensen
 */
public class GalleryEditPanel extends AbsolutePanel // AbsolutePanel needed to support dnd
{
    public static final String EDIT_ACTION = "editgallery";

    public static final String CREATE_ACTION = "creategallery";

    public GalleryEditPanel ()
    {
        // create a new gallery
        _newGallery = true;
        GalleryData galleryData = new GalleryData();
        galleryData.gallery = new Gallery();
        galleryData.gallery.name = ""; // TODO a decent new gallery name default
        galleryData.gallery.description = "";
        galleryData.photos = new ArrayList<Photo>();
        display(galleryData);
    }

    public GalleryEditPanel (int galleryId)
    {
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData result)
            {
                display(result);
            }
        });
    }

    protected void display (GalleryData galleryData)
    {
        _galleryData = galleryData;
        addStyleName("Gallery");

        // add editable gallery detail panel
        add(new GalleryDetailPanel(_galleryData.gallery, false), 0, 0);

        // add "save" button
        PushButton saveButton = MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.saveButton(),
            new ClickListener() {
                public void onClick (Widget sender) {
                    final PushButton button = (PushButton) sender;
                    button.setEnabled(false);
                    if (_newGallery) {
                        _gallerysvc.createGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                            new MsoyCallback<Gallery>() {
                                public void onSuccess (Gallery result) {
                                    _newGallery = false;
                                    _galleryData.gallery = result;
                                    button.setEnabled(true);
                                }
                            }
                        );
                    } else {
                        _gallerysvc.updateGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                            new MsoyCallback<Void>() {
                                public void onSuccess (Void result) {
                                    button.setEnabled(true);
                                }
                            }
                        );
                    }
                }
            }
        );

         saveButton.addStyleName("Save");
         add(saveButton, 25, 400);

        // TODO add drop panel for organizing this gallery, pass in List of Photos

        // TODO show photos that the member owns

    }

    protected boolean _newGallery;
    protected GalleryData _galleryData;

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
