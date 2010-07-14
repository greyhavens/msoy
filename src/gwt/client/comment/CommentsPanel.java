//
// $Id$

package client.comment;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.gwt.CommentServiceAsync;
import com.threerings.msoy.data.all.MediaDescSize;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.ComplainPopup;
import client.ui.MsoyUI;
import client.ui.SafeHTML;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.MsoyPagedServiceDataModel;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends PagedGrid<Comment>
{
    public CommentsPanel (int entityType, int entityId, boolean rated)
    {
        this(entityType, entityId, Comment.COMMENTS_PER_PAGE, rated);
    }

    public CommentsPanel (int entityType, int entityId, int commentsPerPage, boolean rated)
    {
        super(commentsPerPage, 1, NAV_ON_BOTTOM);
        addStyleName("CommentsPanel");
        addStyleName("dottedGrid");
        setCellAlignment(HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);

        _rated = rated;
        _etype = entityType;
        _entityId = entityId;

        add(new Label(_cmsgs.loadingComments()));
        setModel(new CommentModel(), 0);
    }

    public void showPostPopup ()
    {
        if (!MsoyUI.requireValidated()) {
            return;
        }
        _post.setEnabled(false);
        new PostPanel().show();
    }

    @Override
    protected Widget createContents (int start, int count, List<Comment> list)
    {
        if (_batchDelete != null) {
            _batchDelete.clear();
        }
        return super.createContents(start, count, list);
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

        // if we're a validated member, display a button for posting a comment
        if (CShell.isRegistered()) {
            _post = new Button(_cmsgs.postComment(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    showPostPopup();
                }
            });
            controls.setWidget(0, 0, _post);
        }

        if (commentsCanBeBatchDeleted()) {
            Button batchButton = new Button("Delete Checked");
            _batchDelete = new DeleteClickCallback(batchButton, "Are you sure you want to delete these comments?");
            controls.setWidget(0, 1, batchButton);
        }
    }

    /**
     * Returns the size of thumbnail image to use next to our comments.
     */
    protected int getThumbnailSize ()
    {
        return MediaDescSize.HALF_THUMBNAIL_SIZE;
    }

    protected boolean commentsCanBeRated ()
    {
        return _rated;
    }

    protected boolean commentsCanBeBatchDeleted ()
    {
        return CShell.isSupport() ||
            (_etype == Comment.TYPE_PROFILE_WALL && _entityId == CShell.getMemberId());
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
     * Returns true if the viewer should be shown the text of the supplied comment.
     */
    protected boolean shouldDisplay (Comment comment)
    {
        return comment.currentRating >= Comment.RATED_HIDDEN;
    }

    /**
     * Returns true if the viewer should be shown the text of the supplied comment.
     */
    protected boolean shouldEmphasize (Comment comment)
    {
        return comment.currentRating >= Comment.RATED_EMPHASIZED;
    }

    /**
     * Returns true if the viewer can complain about the supplied comment.
     */
    protected boolean canComplain (Comment comment)
    {
        return CShell.isValidated() && (CShell.getMemberId() != comment.commentor.getMemberId());
    }

    protected void postComment (String text)
    {
        _commentsvc.postComment(_etype, _entityId, text, new InfoCallback<Comment>() {
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

    protected void rateComment (
        final Comment comment, final boolean rating, InfoCallback<Integer> callback)
    {
        _commentsvc.rateComment(_etype, _entityId, comment.posted, rating, callback);
    }

    protected Command deleteComment (final Comment comment)
    {
        return new Command() {
            public void execute () {
                List<Long> single = new LinkedList<Long>(Collections.singleton(comment.posted));
                _commentsvc.deleteComments(_etype, _entityId, single, new InfoCallback<Integer>() {
                    public void onSuccess (Integer deleted) {
                        if (deleted > 0) {
                            MsoyUI.info(_cmsgs.commentDeleted());
                            _commentCount = -1;
                            removeItem(comment);
                            _batchDelete.remove(comment);
                        } else {
                            MsoyUI.error(_cmsgs.commentDeletionNotAllowed());
                        }
                    }
                });
            }
        };
    }

    protected void setDeletionCheckbox (Comment comment, boolean checked)
    {
        if (checked) {
            _batchDelete.add(comment);
        } else {
            _batchDelete.remove(comment);
        }
        _batchDelete.updateAbledness();
    }

    protected void complainComment (Comment comment)
    {
        new CommentComplainPopup(comment, _etype, _entityId).show();
    }

    protected static boolean mayPostComment (String comment)
    {
        comment = comment.trim();
        return comment.length() >= 8;
    }

    protected class DeleteClickCallback extends ClickCallback<Integer>
    {
        protected DeleteClickCallback (HasClickHandlers trigger, String confirmMessage)
        {
            super(trigger, confirmMessage);
            updateAbledness();
        }

        public void add (Comment comment)
        {
            _batchComments.put(comment.posted, comment);
            updateAbledness();
        }

        public void remove (Comment comment)
        {
            _batchComments.remove(comment.posted);
            updateAbledness();
        }

        public void clear () {
            _batchComments.clear();
            updateAbledness();
        }

        public void updateAbledness () {
            super.setEnabled(!_batchComments.isEmpty());
        }

        @Override protected boolean callService () {
            _commentsvc.deleteComments(_etype, _entityId,
                new LinkedList<Long>(_batchComments.keySet()), this);
            return true;
        }

        @Override protected boolean gotResult (Integer result) {
            for (Comment comment : _batchComments.values()) {
                removeItem(comment);
            }
            _commentCount = -1;
            _batchComments.clear();
            return false;
        }

        protected Map<Long, Comment> _batchComments = new HashMap<Long, Comment>();
    }

    protected class CommentModel extends MsoyPagedServiceDataModel<Comment, PagedResult<Comment>>
    {
        @Override
        protected void callFetchService (int start, int count, boolean needCount,
                                         AsyncCallback<PagedResult<Comment>> callback) {
            _commentsvc.loadComments(_etype, _entityId, start, count, needCount, callback);
        }
    }

    protected class PostPanel extends BorderedDialog
    {
        public PostPanel () {
            super(false, false, false);
            setHeaderTitle(_cmsgs.commentPostTitle());

            FlowPanel contents = MsoyUI.createFlowPanel("PostComment");
            contents.add(new Label(_cmsgs.commentText()));
            contents.add(_text = new TextArea());
            _text.setWidth("450px");
            _text.setVisibleLines(3);
            contents.add(MsoyUI.createLabel("", "clear")); // forces a line break
            contents.add(_agree = new CheckBox(_cmsgs.commentAmNotAsshole(), true));
            SafeHTML.fixAnchors(_agree.getElement()); // doctor up the external link
            setContents(contents);

            addButton(new Button(_cmsgs.cancel(), onCancel()));
            addButton(new Button(_cmsgs.send(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    if (!_agree.getValue()) {
                        MsoyUI.errorNear(_cmsgs.commentMustNotBeAsshole(), _agree);
                        return;
                    }
                    hide(); // hide now, if they fail validation, they get to type everything anew

                    String text = _text.getText();
                    if (!mayPostComment(text)) {
                        MsoyUI.error(_cmsgs.commentInvalid());
                        return;
                    }
                    postComment(text);
                }
            }));
        }

        protected void onClosed (boolean autoClosed)
        {
            _post.setEnabled(true);
        }

        protected TextArea _text;
        protected CheckBox _agree;
    }

    protected class CommentComplainPopup extends ComplainPopup
    {
        public CommentComplainPopup (Comment comment, int type, int id)
        {
            super(CommentService.MAX_COMPLAINT_LENGTH);
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

    protected DeleteClickCallback _batchDelete;

    protected boolean _rated;
    protected Button _post;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final CommentServiceAsync _commentsvc = GWT.create(CommentService.class);
}
