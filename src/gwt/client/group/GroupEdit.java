//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
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

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;

import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;
import client.util.WebContext;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends DialogBox
{
    /**
     * A callback interface for classes that want to know when a group successfully committed.
     */
    public static interface GroupSubmissionListener {
        public void groupSubmitted(Group group);
    }
    
    public GroupEdit (WebContext ctx, Group group, GroupSubmissionListener listener)
    {
        super();
        setPopupPosition(30, 30);
        setText("Group Editor");
        _ctx = ctx;
        _group = group;
        _listener = listener;
        setStyleName("groupPopup");

        _content = new DockPanel();
        setWidget(_content);
        
        _table = new HeaderValueTable();
        _content.add(_table, DockPanel.CENTER);

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        _content.add(_errorContainer, DockPanel.NORTH);

        HorizontalPanel bpanel = new HorizontalPanel();
        _content.add(bpanel, DockPanel.SOUTH);

        bpanel.add(_esubmit = new Button(group.groupId == 0 ? "Create" : "Commit"));
        _esubmit.setStyleName("groupEditorButton");
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });
        updateSubmittable();
        
        Button ecancel = new Button("Cancel");
        bpanel.add(ecancel);
        ecancel.setStyleName("groupEditorButton");
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        });

        _table.addHeader((group.groupId == 0 ? "Creating" : "Editing") + " Group");

        // name field
        final TextBox nameBox = new TextBox();
        nameBox.setText(group.name != null ? group.name : "");
        nameBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _group.name = nameBox.getText().trim();
                updateSubmittable();
            }
        });
        _table.addRow("Name", nameBox);

        // homepage url field
        final TextBox urlBox = new TextBox();
        urlBox.setMaxLength(255);
        urlBox.setText(group.homepageUrl != null ? group.homepageUrl : "");
        urlBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _group.homepageUrl = urlBox.getText().trim();
                updateSubmittable();
            }
        });
        _table.addRow("Homepage URL", urlBox);

        // blurb field
        final TextBox blurbBox = new TextBox();
        blurbBox.setMaxLength(80);
        blurbBox.setText(group.blurb != null ? group.blurb : "");
        blurbBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _group.blurb = blurbBox.getText().trim();
                updateSubmittable();
            }
        });
        _table.addRow("Blurb", blurbBox);

        // charter field
        final TextArea charterArea = new TextArea();
        charterArea.setCharacterWidth(80);
        charterArea.setVisibleLines(10);
        charterArea.setText(group.charter != null ? group.charter : "");
        charterArea.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _group.charter = charterArea.getText().trim();
            }
        });
        _table.addRow("Charter", charterArea);

        // policy field
        final ListBox policyBox = new ListBox();
        policyBox.addItem("Public");
        policyBox.addItem("Invitation Only");
        policyBox.addItem("Exclusive");
        policyBox.setSelectedIndex(_group.policy == Group.POLICY_PUBLIC ? 0 :
                                   _group.policy == Group.POLICY_INVITE_ONLY ? 1 : 2);
        policyBox.addChangeListener(new ChangeListener() {
           public void onChange (Widget sender) {
               switch(policyBox.getSelectedIndex()) {
               case 0: _group.policy = Group.POLICY_PUBLIC; break;
               case 1: _group.policy = Group.POLICY_INVITE_ONLY; break;
               case 2: _group.policy = Group.POLICY_EXCLUSIVE; break;
               }
           }
        });
        _table.addRow("Policy", policyBox);

        // image fields
        HorizontalPanel imagePanel = new HorizontalPanel();
        _table.addRow("Images", imagePanel);
        int types[] = { IMAGE_LOGO, IMAGE_INFO_BACKGROUND, IMAGE_DETAIL_BACKGROUND, 
            IMAGE_PEOPLE_BACKGROUND };
        String labels[] = { "Logo", "Info Background", "Detail Background", "People Background" };
        for (int i = 0; i < types.length; i++) {
            VerticalPanel imageBox = new VerticalPanel();
            imagePanel.add(imageBox);
            updateImageBox(imageBox, types[i], "Set " + labels[i]);
        }
    }

    // called when a group's name is changed, to determine the enabled-ness of the submit button
    protected void updateSubmittable ()
    {
        // TODO: formalize sanity check(s)
        if (_group.name != null && _group.name.length() > 3 && _group.name.length() < 24) {
            _esubmit.setEnabled(true);
        }
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
                if (_listener != null) {
                    _listener.groupSubmitted(_group);
                }
                hide();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to commit group: " + caught.getMessage());
            }
        };
        if (_group.groupId > 0) {
            _ctx.groupsvc.updateGroup(_ctx.creds, _group, callback);
        } else {
            _ctx.groupsvc.createGroup(_ctx.creds, _group, callback);
        }
    }
    
    // update the contents of the image box, e.g. after the image has been changed
    protected void updateImageBox (final CellPanel box, final int type, final String buttonLabel)
    {
        MediaDesc media = null;
        switch (type) {
        case IMAGE_LOGO: media = _group.logo; break;
        case IMAGE_INFO_BACKGROUND: media = _group.infoBackground; break;
        case IMAGE_DETAIL_BACKGROUND: media = _group.detailBackground; break;
        case IMAGE_PEOPLE_BACKGROUND: media = _group.peopleBackground; break;
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
                        _group.infoBackground = photo.photoMedia;
                        break;
                    case IMAGE_DETAIL_BACKGROUND:
                        _group.detailBackground = photo.photoMedia;
                        break;
                    case IMAGE_PEOPLE_BACKGROUND:
                        _group.peopleBackground = photo.photoMedia;
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
    }
    
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
    protected GroupSubmissionListener _listener;
    protected Button _esubmit;  
    protected List _images;
    
    protected DockPanel _content;
    protected HeaderValueTable _table;
    protected VerticalPanel _errorContainer;

    // static final fields used to decide which image we're working with.
    protected static final int IMAGE_LOGO = 1;
    protected static final int IMAGE_INFO_BACKGROUND = 2;
    protected static final int IMAGE_DETAIL_BACKGROUND = 3;
    protected static final int IMAGE_PEOPLE_BACKGROUND = 4;
}
