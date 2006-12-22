//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupName;
import com.threerings.msoy.web.data.GroupExtras;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;
import client.util.WebContext;
import client.util.BorderedDialog;
import client.util.MsoyUI;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when a group successfully committed.
     */
    public static interface GroupSubmissionListener {
        public void groupSubmitted(Group group);
    }

    /**
     * This constructor is for creating new Groups.  
     */
    public GroupEdit (WebContext ctx) 
    {
        this(ctx, new Group(), new GroupExtras(), null);
    }
    
    public GroupEdit (WebContext ctx, Group group, GroupExtras extras,  
        GroupSubmissionListener listener)
    {
        super();
        _ctx = ctx;
        _group = group;
        _extras = extras;
        _listener = listener;
       
        _header.add(MsoyUI.createLabel(_group.groupId == 0 ? "Create new group" :
            "Edit group", "GroupTitle"));
        VerticalPanel contents = (VerticalPanel)_contents;

        _errorContainer = new HorizontalPanel();
        contents.add(_errorContainer);

        HorizontalPanel nameField = new HorizontalPanel();
        nameField.add(new InlineLabel("Group Name"));
        TextBox nameBox = new TextBox();
        nameBox.setMaxLength(GroupName.LENGTH_MAX);
        nameBox.setVisibleLength(20);
        if (_group.name != null) {
            nameBox.setText(_group.name);
        }
        nameField.add(nameBox);
        contents.add(nameField);

        TabPanel groupTabs = new TabPanel();
        groupTabs.setStyleName("Tabs");
        groupTabs.add(createInfoPanel(), "Information");
        groupTabs.add(createDescriptionPanel(), "Description");
        groupTabs.add(createBackgroundsPanel(), "Background Images");
        groupTabs.selectTab(0);
        contents.add(groupTabs);
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components
    // that depend on members that are set in this object's constructor can be used here.
    public Widget createContents () 
    {
        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("groupEditor");
        return contents;
    }

    protected Panel createInfoPanel ()
    {
        return new VerticalPanel();
    }

    protected Panel createDescriptionPanel ()
    {
        return new VerticalPanel();
    }

    protected Panel createBackgroundsPanel ()
    {
        return new VerticalPanel();
    }

    // submit a modified group, and notify listeners
    protected void commitEdit ()
    {
        // check if the group name is valid.
        if (!Character.isLetter(_group.name.charAt(0)) &&
            !Character.isDigit(_group.name.charAt(0))) {
            Window.alert("The group name must start with a character or number!");
            return;
        }
        
        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                if (_listener != null) {
                    _listener.groupSubmitted(_group);
                } else if (_group.groupId == 0) {
                    // new group created - go to the new group view page
                    Group newGroup = (Group)result;
                    History.newItem("" + newGroup.groupId);
                }
            }
            public void onFailure (Throwable caught) {
                addError("Failed to commit group: " + caught.getMessage());
            }
        };
        if (_group.groupId > 0) {
            _ctx.groupsvc.updateGroup(_ctx.creds, _group, _extras, callback);
        } else {
            _ctx.groupsvc.createGroup(_ctx.creds, _group, _extras, callback);
        }
    }
    
    /*// update the contents of the image box, e.g. after the image has been changed
    protected void updateImageBox (final CellPanel box, final int type, final String buttonLabel)
    {
        MediaDesc media = null;
        switch (type) {
        case IMAGE_LOGO: media = _group.logo; break;
        case IMAGE_INFO_BACKGROUND: media = _extras.infoBackground; break;
        case IMAGE_DETAIL_BACKGROUND: media = _extras.detailBackground; break;
        case IMAGE_PEOPLE_BACKGROUND: media = _extras.peopleBackground; break;
        default: addError("Internal Error! Unknown image type: " + type); return;
        }

        box.clear();
        if (media != null) {
            box.add(new Image(MsoyEntryPoint.toMediaPath(media.getMediaPath())));
        }
        Button changeButton = new Button(buttonLabel);
        changeButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                popupImageChooser(box, type, buttonLabel);
            }
        });
        box.add(changeButton);
    }

    // pop up a scrollable horizontal list of photo items from which to choose a logo
    protected void popupImageChooser (final CellPanel box, final int type, final String buttonLabel)
    {
        // the list of images is cached for this object
        if (_images == null) {
            _ctx.itemsvc.loadInventory(_ctx.creds, Item.PHOTO, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _images = (List) result;
                    // will use the cached results this time.
                    popupImageChooser(box, type, buttonLabel);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadInventory failed", caught);
                    // TODO: if ServiceException, translate
                    addError("Failed to load photo inventory for logo selection.");
                }
            });
        } else {
            if (_images.size() == 0) {
                addError("Upload some photos to your inventory to choose an image.");
                return;
            }
            // create the popup and its nested panels
            HorizontalPanel itemPanel = new HorizontalPanel();
            ScrollPanel chooser = new ScrollPanel(itemPanel);
            final PopupPanel popup = new PopupPanel(true);
            popup.setStyleName("groupLogoPopup");
            popup.setWidget(chooser);

            // set up a listener to pick an image for the logo, hide the popup, and update
            final ClickListener logoChanger = new ClickListener() {
                public void onClick (Widget sender) {
                    Photo photo = ((PhotoThumbnailImage) sender).photo;
                    switch(type) {
                    case IMAGE_LOGO: 
                        _group.logo = photo.getThumbnailMedia();
                        break;
                    case IMAGE_INFO_BACKGROUND:
                        _extras.infoBackground = photo.photoMedia;
                        break;
                    case IMAGE_DETAIL_BACKGROUND:
                        _extras.detailBackground = photo.photoMedia;
                        break;
                    case IMAGE_PEOPLE_BACKGROUND:
                        _extras.peopleBackground = photo.photoMedia;
                        break;
                    default:
                        addError("Internal Error! Unkown image type: " + type);
                    }
                    popup.hide();
                    updateImageBox(box, type, buttonLabel);
                }
            };

            // iterate over all our photos and fill the popup panel
            Iterator i = _images.iterator();
            while (i.hasNext()) {
                Image image = new PhotoThumbnailImage(((Photo) i.next()));
                image.addClickListener(logoChanger);
                itemPanel.add(image);
            }

            // finally show the popup
            popup.show();
        }
    }*/
    
    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    /**
     * A tiny helper class that carries a Photo in a Widget.
     */
    protected static class PhotoThumbnailImage extends Image {
        public Photo photo;
        protected PhotoThumbnailImage (Photo photo)
        {
            super(MsoyEntryPoint.toMediaPath(photo.getThumbnailPath()));
            this.photo = photo;
        }
    }

    protected WebContext _ctx;
    protected Group _group;
    protected GroupExtras _extras;
    protected GroupSubmissionListener _listener;
    protected List _images;

    protected HorizontalPanel _errorContainer;

    // static final fields used to decide which image we're working with.
    protected static final int IMAGE_LOGO = 1;
    protected static final int IMAGE_INFO_BACKGROUND = 2;
    protected static final int IMAGE_DETAIL_BACKGROUND = 3;
    protected static final int IMAGE_PEOPLE_BACKGROUND = 4;
}
