//
// $Id$

package client.person;

import java.util.Map;

import client.mail.MailBodyObjectComposer;
import client.mail.MailBodyObjectDisplay;

import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailBodyObject;

public abstract class FriendInvite
{
    public static final class Composer
        implements MailBodyObjectComposer
    {
        // @Override
        public Widget widgetForComposition (WebContext ctx)
        {
            // TODO Auto-generated method stub
            return null;
        }
        // @Override
        public MailBodyObject getComposedObject ()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    public static final class FriendBodyObject extends MailBodyObjectDisplay
    {
        public FriendBodyObject ()
        {
            // TODO
        }
        
        public FriendBodyObject (Map state)
        {
            super();
        }

        // @Override
        public Widget widgetForRecipient (WebContext ctx)
        {
            // TODO Auto-generated method stub
            return null;
        }

        // @Override
        public Widget widgetForOthers (WebContext ctx)
        {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
