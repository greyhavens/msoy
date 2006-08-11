//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.PersonLayout;

/**
 * Provides the server implementation of {@link PersonService}.
 */
public class PersonServlet extends RemoteServiceServlet
    implements PersonService
{
    // from interface PersonService
    public ArrayList loadBlurbs (int memberId)
    {
        ArrayList<Object> data = new ArrayList<Object>();
        data.add(createDefaultLayout());
        return data;
    }

    protected PersonLayout createDefaultLayout ()
    {
        PersonLayout layout = new PersonLayout();
        layout.layout = PersonLayout.ONE_COLUMN_LAYOUT;

        ArrayList<BlurbData> blurbs = new ArrayList<BlurbData>();
        BlurbData blurb = new BlurbData();
        blurb.type = BlurbData.PROFILE;
        blurb.blurbId = 0;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.FRIENDS;
        blurb.blurbId = 1;
        blurbs.add(blurb);
        layout.blurbs = blurbs;

        return layout;
    }
}
