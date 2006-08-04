//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.data.Profile;

import client.MsoyEntryPoint;

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
        getFlexCellFormatter().setColSpan(0, 1, 2);
        setWidget(1, 1, _headline = new Label("headline"));
        setWidget(2, 1, _homepage = new HTML("homepage"));
        setWidget(3, 1, _laston = new Label("laston"));

        setWidget(1, 2, _blog = new HTML("blog"));
        setWidget(2, 2, _gallery = new HTML("gallery"));
        setWidget(3, 2, _hood = new HTML("hood"));
    }

    public void setProfile (Profile profile)
    {
        if (profile.photo != null) {
            _photo.setUrl(
                MsoyEntryPoint.toMediaPath(profile.photo.getThumbnailPath()));
        }
        _name.setText(profile.displayName);
        _headline.setText(profile.headline);
        _homepage.setHTML("<a href=\"" + profile.homePageURL + "\">" +
                          profile.homePageURL + "</a>");
    }

    protected Image _photo;
    protected HTML _homepage;
    protected Label _name, _headline, _laston;
    protected HTML _blog, _gallery, _hood;
}
