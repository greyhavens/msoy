//
// $Id$

package client.person;

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

import client.MsoyEntryPoint;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new FlexTable();

        _content.setWidget(0, 0, _photo = new Image());
        _content.getFlexCellFormatter().setRowSpan(0, 0, 4);

        _content.setWidget(0, 1, _name = new Label("name"));
        _name.setStyleName("profile_name");
        _content.getFlexCellFormatter().setColSpan(0, 1, 2);
        _content.setWidget(1, 1, _headline = new Label("headline"));
        _headline.setStyleName("profile_headline");
        _content.setWidget(2, 1, _homepage = new HTML("homepage"));
        _content.setWidget(3, 1, _laston = new Label("..."));

        _content.setWidget(1, 2, _blog = new HTML(""));
        _content.setWidget(2, 2, _gallery = new HTML(""));
        // setWidget(3, 2, _hood = new HTML(""));
        _content.setWidget(3, 2, _edit = new Button("Edit"));
        _edit.addClickListener(new ClickListener() {
            public void onClick (Widget source) {
                startEdit();
                _edit.setEnabled(false);
            }
        });

        return _content;
    }

    // @Override // from Blurb
    protected void didInit (Object blurbData)
    {
        _profile = (Profile)blurbData;
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
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        _name.setText("Failed to load profile data: " + cause);
    }

    protected void startEdit ()
    {
        if (_profile == null) {
            return; // nothing doing
        }

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

    protected FlexTable _content;

    protected Profile _profile;
    protected Image _photo;
    protected HTML _homepage;
    protected Label _name, _headline, _laston;
    protected HTML _blog, _gallery, _hood;

    protected Button _edit;
    protected TextBox _ename, _eheadline, _ehomepage;
}
