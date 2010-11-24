//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.profile.gwt.ProfileService;

import client.comment.CommentsPanel;
import client.shell.ShellMessages;

/**
 * Displays a comment wall on a member's profile.
 */
public class CommentsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        setHeader(_msgs.commentsTitle());
        setContent(_wall = new WallPanel(pdata.name.getId()));
    }

    protected class WallPanel extends CommentsPanel
    {
        public WallPanel (int memberId) {
            super(CommentType.PROFILE_WALL, memberId, COMMENTS_PER_PAGE, false);
            addStyleName("Wall");
            removeStyleName("dottedGrid");
            setVisible(true); // trigger immediate loading of our model
        }

        @Override // from CommentsPanel
        protected int getThumbnailSize() {
            return MediaDescSize.HALF_THUMBNAIL_SIZE;
        }
    }

    protected WallPanel _wall;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final int COMMENTS_PER_PAGE = 20;
}
