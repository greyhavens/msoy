//
// $Id$

package client.profile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Profile;

import client.shell.MsoyEntryPoint;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new FlexTable();
       _content.setStyleName("profileBlurb");

        _content.setWidget(0, 0, _photo = new Image());
        _content.getFlexCellFormatter().setRowSpan(0, 0, 4);

        _content.setWidget(0, 1, _name = new Label("name"));
        _name.setStyleName("Name");
        _content.getFlexCellFormatter().setColSpan(0, 1, 2);
        _content.setWidget(1, 1, _headline = new Label("headline"));
        _headline.setStyleName("Headline");
        _content.setWidget(2, 1, _homepage = new HTML("homepage"));
        _content.setWidget(3, 1, _laston = new Label("..."));

        _content.setWidget(1, 2, _blog = new HTML(""));
        _content.setWidget(2, 2, _gallery = new HTML(""));
        // setWidget(3, 2, _hood = new HTML(""));
        _edit = new Button("Edit");
        _edit.addClickListener(new ClickListener() {
            public void onClick (Widget source) {
                if (_editing) {
                    commitEdit();
                } else {
                    startEdit();
                }
            }
        });

        return _content;
    }

    // @Override // from Blurb
    protected void didInit (Object blurbData)
    {
        setHeader("Profile");

        _profile = (Profile)blurbData;
        displayProfile();
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader("Error");
        _name.setText("Failed to load profile data: " + cause);
    }

    protected void startEdit ()
    {
        if (_profile == null) {
            return; // nothing doing
        }

        // switch to update mode
        _edit.setText("Done");
        _editing = true;

        if (_ename == null) {
            _ename = new TextBox();
            _eheadline = new TextBox();
            _eheadline.setVisibleLength(50);
            _ehomepage = new TextBox();
            _ehomepage.setVisibleLength(50);
        }

        _ename.setText(_profile.displayName);
        _eheadline.setText(_profile.headline);
        _ehomepage.setText(_profile.homePageURL);
        _content.setWidget(0, 1, _ename);
        _content.setWidget(1, 1, _eheadline);
        _content.setWidget(2, 1, _ehomepage);
    }

    protected void displayProfile ()
    {
        if (_profile.photo != null) {
            _photo.setUrl(
                MsoyEntryPoint.toMediaPath(_profile.photo.getThumbnailPath()));
        }
        _name.setText(_profile.displayName);
        _headline.setText(_profile.headline);
        _homepage.setHTML("<a href=\"" + _profile.homePageURL + "\">" +
            _profile.homePageURL + "</a>");

        _content.setWidget(0, 1, _name);
        _content.setWidget(1, 1, _headline);
        _content.setWidget(2, 1, _homepage);

        // display the edit button if this is our profile
        CProfile.log("Creds " + CProfile.creds);
        if (_profile.memberId == CProfile.creds.getMemberId()) {
            _content.setWidget(3, 2, _edit);
        }
    }

    protected void commitEdit ()
    {
        // go back to edit mode
        _edit.setText("Edit");
        _editing = false;

        // configure our profile instance with their bits
        _profile.displayName = _ename.getText();
        _profile.headline = _eheadline.getText();
        _profile.homePageURL = _ehomepage.getText();

        CProfile.profilesvc.updateProfile(CProfile.creds, _profile, new AsyncCallback() {
            public void onSuccess (Object result) {
                GWT.log("Yay!", null);
                displayProfile();
            }
            public void onFailure (Throwable cause) {
                GWT.log("Nay!", cause);
            }
        });
    }

    protected FlexTable _content;
    protected boolean _editing = false;

    protected Profile _profile;
    protected Image _photo;
    protected HTML _homepage;
    protected Label _name, _headline, _laston;
    protected HTML _blog, _gallery, _hood;

    protected Button _edit;
    protected TextBox _ename, _eheadline, _ehomepage;
}
