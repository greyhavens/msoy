//
// $Id$

package client.comment;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.gwt.CommentServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.ComplainPopup;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.MsoyCallback;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends PagedGrid<Comment>
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

        add(new Label(_cmsgs.loadingComments()));
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (visible && _model == null) {
            setModel(new CommentModel(), 0);
        }
    }

    @Override // from PagedGrid
    protected Widget createWidget (Comment comment)
    {
        return new CommentPanel(this, comment);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _cmsgs.noComments();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always need our navigation because it has the "post" button
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // if we're logged in, display a button for posting a comment
        if (!CShell.isGuest()) {
            _post = new Button(_cmsgs.postComment(), new ClickListener() {
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
        return (_etype != Comment.TYPE_PROFILE_WALL) && !CShell.isGuest() &&
            (CShell.getMemberId() != comment.commentor.getMemberId());
    }

    protected void showPostPanel ()
    {
        add(new PostPanel());
    }

    protected void postComment (String text)
    {
        _commentsvc.postComment(_etype, _entityId, text, new MsoyCallback<Comment>() {
            public void onSuccess (Comment result) {
                postedComment(result);
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
            MsoyUI.info(_cmsgs.commentPosted());
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
                _commentsvc.deleteComment(
                    _etype, _entityId, comment.posted, new MsoyCallback<Boolean>() {
                    public void onSuccess (Boolean deleted) {
                        if (deleted) {
                            MsoyUI.info(_cmsgs.commentDeleted());
                            _commentCount = -1;
                            removeItem(comment);
                        } else {
                            MsoyUI.error(_cmsgs.commentDeletionNotAllowed());
                        }
                    }
                });
            }
        };
    }

    protected void complainComment (Comment comment)
    {
        new CommentComplainPopup(comment, _etype, _entityId).show();
    }

    protected class CommentModel
        extends ServiceBackedDataModel<Comment, CommentService.CommentResult>
    {
        @Override
        protected void callFetchService (int start, int count, boolean needCount, AsyncCallback<CommentService.CommentResult> callback) {
            _commentsvc.loadComments(_etype, _entityId, start, count, needCount, callback);
        }
        @Override
        protected int getCount (CommentService.CommentResult result) {
            return result.commentCount;
        }
        @Override
        protected List<Comment> getRows (CommentService.CommentResult result) {
            return result.comments;
        }
    }

    protected class PostPanel extends VerticalPanel
    {
        public PostPanel () {
            add(new Label(_cmsgs.commentText()));
            add(_text = new TextArea());
            _text.setCharacterWidth(40);
            _text.setVisibleLines(3);
            add(_status = new Label(""));
            RowPanel buttons = new RowPanel();
            buttons.add(new Button(_cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearPostPanel(PostPanel.this);
                }
            }));
            buttons.add(new Button(_cmsgs.send(), new ClickListener() {
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
            _commentsvc.complainComment(_description.getText(), _type, _id, _comment.posted, this);
            return true;
        }

        protected Comment _comment;
        protected int _type, _id;
    }

    protected int _etype, _entityId;
    protected int _commentCount = -1;

    protected VerticalPanel _comments;
    protected Button _post;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final CommentServiceAsync _commentsvc = (CommentServiceAsync)
        ServiceUtil.bind(GWT.create(CommentService.class), CommentService.ENTRY_POINT);
}
