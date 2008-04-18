//
// $Id$

package client.shell;

import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.CommentService;

import client.msgs.ComplainPopup;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;
import client.util.ServiceBackedDataModel;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends PagedGrid
{
    public CommentsPanel (int entityType, int entityId)
    {
        this(entityType, entityId, Comment.COMMENTS_PER_PAGE);
    }

    public CommentsPanel (int entityType, int entityId, int commentsPerPage)
    {
        super(commentsPerPage, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setCellAlignment(HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);

        _etype = entityType;
        _entityId = entityId;

        add(new Label(CShell.cmsgs.loadingComments()));
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (visible && _model == null) {
            setModel(new CommentModel(), 0);
        }
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new CommentPanel(this, (Comment)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CShell.cmsgs.noComments();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always need our navigation because it has the "post" button
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // if we're logged in, display a button for posting a comment
        if (CShell.getMemberId() != 0) {
            _post = new Button(CShell.cmsgs.postComment(), new ClickListener() {
                public void onClick (Widget sender) {
                    _post.setEnabled(false);
                    showPostPanel();
                }
            });
            controls.setWidget(0, 0, _post);
        }
    }

    /**
     * Returns the size of thumbnail image to use next to our comments.
     */
    protected int getThumbnailSize ()
    {
        return MediaDesc.THUMBNAIL_SIZE;
    }

    /**
     * Returns true if the viewer can delete the supplied comment.
     */
    protected boolean canDelete (Comment comment)
    {
        return CShell.isSupport() || Comment.canDelete(
            _etype, _entityId, comment.commentor.getMemberId(), CShell.getMemberId());
    }

    /**
     * Returns true if the viewer can complain about the supplied comment.
     */
    protected boolean canComplain (Comment comment)
    {
        return (_etype != Comment.TYPE_PROFILE_WALL) && (CShell.getMemberId() != 0) &&
            (CShell.getMemberId() != comment.commentor.getMemberId());
    }

    protected void showPostPanel ()
    {
        add(new PostPanel());
    }

    protected void postComment (String text)
    {
        CShell.commentsvc.postComment(CShell.ident, _etype, _entityId, text, new MsoyCallback() {
            public void onSuccess (Object result) {
                postedComment((Comment)result);
            }
        });
    }

    protected void postedComment (Comment comment)
    {
        if (_page == 0) {
            ((CommentModel)_model).prependItem(comment);
            _commentCount = -1;
            displayPage(0, true);
        } else {
            MsoyUI.info(CShell.cmsgs.commentPosted());
        }
    }

    protected void clearPostPanel (PostPanel panel)
    {
        remove(panel);
        _post.setEnabled(true);
    }

    protected Command deleteComment (final Comment comment)
    {
        return new Command() {
            public void execute () {
                CShell.commentsvc.deleteComment(
                    CShell.ident, _etype, _entityId, comment.posted, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        MsoyUI.info(CShell.cmsgs.commentDeleted());
                        _commentCount = -1;
                        removeItem(comment);
                    }
                });
            }
        };
    }

    protected void complainComment (Comment comment)
    {
        new CommentComplainPopup(comment, _etype, _entityId).show();
    }

    protected class CommentModel extends ServiceBackedDataModel
    {
        protected void callFetchService (int start, int count, boolean needCount) {
            CShell.commentsvc.loadComments(_etype, _entityId, start, count, needCount, this);
        }
        protected int getCount (Object result) {
            return ((CommentService.CommentResult)result).commentCount;
        }
        protected List getRows (Object result) {
            return ((CommentService.CommentResult)result).comments;
        }
    }

    protected class PostPanel extends VerticalPanel
    {
        public PostPanel () {
            add(new Label(CShell.cmsgs.commentText()));
            add(_text = new TextArea());
            _text.setCharacterWidth(40);
            _text.setVisibleLines(3);
            add(_status = new Label(""));
            RowPanel buttons = new RowPanel();
            buttons.add(new Button(CShell.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearPostPanel(PostPanel.this);
                }
            }));
            buttons.add(new Button(CShell.cmsgs.send(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearPostPanel(PostPanel.this);
                    postComment(_text.getText());
                }
            }));
            add(buttons);
        }

        protected TextArea _text;
        protected Label _status;
    }

    protected class CommentComplainPopup extends ComplainPopup
    {
        public CommentComplainPopup (Comment comment, int type, int id)
        {
            super();
            _comment = comment;
            _type = type;
            _id = id;
        }

        protected boolean callService ()
        {
            CShell.commentsvc.complainComment(
                CShell.ident, _description.getText(), _type, _id, _comment.posted, this);
            return true;
        }

        protected Comment _comment;
        protected int _type, _id;
    }

    protected int _etype, _entityId;
    protected int _commentCount = -1;

    protected VerticalPanel _comments;
    protected Button _post;
}
