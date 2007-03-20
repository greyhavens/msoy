//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Profile;

import client.shell.Page;

/**
 * Displays the photo and header bits of a member's profile.
 */
public class HeaderPanel extends FlexTable
{
    public HeaderPanel ()
    {
        setWidget(0, 0, _photo = new Image());
        getFlexCellFormatter().setRowSpan(0, 0, 4);

        setWidget(0, 1, _name = new Label("name"));
        _name.setStyleName("profile_name");
        getFlexCellFormatter().setColSpan(0, 1, 2);
        setWidget(1, 1, _headline = new Label("headline"));
        _headline.setStyleName("profile_headline");
        setWidget(2, 1, _homepage = new HTML("homepage"));
        setWidget(3, 1, _laston = new Label("..."));

        setWidget(1, 2, _blog = new HTML(""));
        setWidget(2, 2, _gallery = new HTML(""));
        // setWidget(3, 2, _hood = new HTML(""));
        setWidget(3, 2, _edit = new Button("Edit"));
        _edit.addClickListener(new ClickListener() {
            public void onClick (Widget source) {
                startEdit();
                _edit.setEnabled(false);
            }
        });
    }

    public void setProfile (Profile profile)
    {
        _profile = profile;
        if (profile.photo != null) {
            _photo.setUrl(profile.photo.getMediaPath());
        }
        _name.setText(profile.displayName);
        _headline.setText(profile.headline);
        _homepage.setHTML(
            "<a href=\"" + profile.homePageURL + "\">" + profile.homePageURL + "</a>");

        setWidget(0, 1, _name);
        setWidget(1, 1, _headline);
        setWidget(2, 1, _homepage);
    }

    public void startEdit ()
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
        setWidget(0, 1, _ename);
        setWidget(1, 1, _eheadline);
        setWidget(2, 1, _ehomepage);
    }

    protected Profile _profile;
    protected Image _photo;
    protected HTML _homepage;
    protected Label _name, _headline, _laston;
    protected HTML _blog, _gallery, _hood;

    protected Button _edit;
    protected TextBox _ename, _eheadline, _ehomepage;
}
