//
// $Id$

package client.msgs;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ListenerList;
import com.threerings.gwt.util.ServiceUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;

import client.shell.CShell;
import client.util.MsoyPagedServiceDataModel;
import client.util.MsoyServiceBackedDataModel;

/**
 * Various data models used by the forum services.
 */
public class ForumModels
{
    /** A data model that provides a particular group's threads. */
    public class GroupThreads
        extends MsoyPagedServiceDataModel<ForumThread, ForumService.ThreadResult>
        implements ThreadContainer
    {
        public GroupThreads (int groupId) {
            _groupId = groupId;
            _group = new GroupName("", _groupId);
        }

        public GroupName getGroup () {
            return _group;
        }

        public boolean canStartThread () {
            return _canStartThread;
        }

        public boolean isManager () {
            return _isManager;
        }

        public boolean isAnnounce () {
            return _isAnnounce;
        }

        /**
         * Are the above calls returning data from the server or just their default constructed
         * values?
         */
        public boolean isFetched () {
            return _fetched;
        }

        /**
         * Get the information from the server to be able to accurately respond to the above
         * requests.
         */
        public void doFetch (final Command onFetched) {
            doFetchRows(0, 1, new AsyncCallback<List<ForumThread>>() {
                public void onFailure (Throwable caught) {
                    CShell.log("doFetch failed", caught);
                }
                public void onSuccess (List<ForumThread> result) {
                    onFetched.execute();
                }
            });
        }

        /**
         * Requests to be informed when we obtain our group name from the first batch of thread
         * results. {@link AsyncCallback#onSuccess} will be called with the {@link GroupName} when
         * we learn it. {@link AsyncCallback#onFailure} will never be called but this interface is
         * more convenient than Command which does not allow us to pass an argument.
         */
        public void addGotNameListener (AsyncCallback<GroupName> onGotGroupName) {
            _gotNameListeners = ListenerList.addListener(_gotNameListeners, onGotGroupName);
            // if we already have our group name, fire the callback immediately
            if (!_group.toString().equals("")) {
                gotGroupName(_group);
            }
        }

        @Override // from ServiceBackedDataModel
        public void prependItem (ForumThread thread) {
            super.prependItem(thread);
            updateThread(thread, this);
        }

        @Override // from ServiceBackedDataModel
        public void appendItem (ForumThread thread) {
            super.appendItem(thread);
            updateThread(thread, this);
        }

        // from ThreadContainer
        public void registerUpdate (ForumThread thread) {
            replaceItem(_pageItems, thread);
        }

        @Override // from ServiceBackedDataModel
        protected void setCurrentResult (ForumService.ThreadResult result)
        {
            _fetched = true;
            _canStartThread = result.canStartThread;
            _isManager = result.isManager;
            _isAnnounce = result.isAnnounce;
            // note all of our threads so that we can provide them later to non-PagedGrid consumers
            for (ForumThread thread : result.page) {
                updateThread(thread, this);
            }
            // grab our real group name from one of our thread records
            if (result.page.size() > 0) {
                gotGroupName(result.page.get(0).group);
            }
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<ForumService.ThreadResult> callback)
        {
            _forumsvc.loadThreads(_groupId, start, count, needCount, callback);
        }

        protected void gotGroupName (GroupName group) {
            _group = group;
            if (_gotNameListeners != null) {
                _gotNameListeners.notify(new ListenerList.Op<AsyncCallback<GroupName>>() {
                    public void notify (AsyncCallback<GroupName> listener) {
                        listener.onSuccess(_group);
                    }
                });
                _gotNameListeners = null;
            }
        }

        protected int _groupId;
        protected GroupName _group;
        protected boolean _fetched;
        protected boolean _canStartThread, _isManager, _isAnnounce;

        protected ListenerList<AsyncCallback<GroupName>> _gotNameListeners;
    }

    /** A data model that provides all threads unread by the authenticated user. */
    public class UnreadThreads extends SimpleDataModel<ForumThread>
        implements ThreadContainer
    {
        public UnreadThreads ()
        {
            super(null);
        }

        // from interface DataModel
        public void doFetchRows (
            final int start, final int count, final AsyncCallback<List<ForumThread>> callback)
        {
            if (_items != null) {
                super.doFetchRows(start, count, callback);
                return;
            }

            _forumsvc.loadUnreadThreads(MAX_UNREAD_THREADS,
                                        new AsyncCallback<List<ForumThread>>() {
                public void onSuccess (List<ForumThread> result) {
                    _items = result;
                    for (ForumThread thread : result) {
                        updateThread(thread, UnreadThreads.this);
                    }
                    doFetchRows(start, count, callback);
                }
                public void onFailure (Throwable failure) {
                    callback.onFailure(failure);
                }
           });
        }

        // from ThreadContainer
        public void registerUpdate (ForumThread thread)
        {
            ForumModels.replaceItem(_items, thread);
        }
    }

    /** A data model that provides all threads with unread posts by friends.  */
    public class UnreadFriendsThreads extends SimpleDataModel<ForumThread>
        implements ThreadContainer
    {
        public UnreadFriendsThreads ()
        {
            super(null);
        }

        // from interface DataModel
        public void doFetchRows (
            final int start, final int count, final AsyncCallback<List<ForumThread>> callback)
        {
            if (_items != null) {
                super.doFetchRows(start, count, callback);
                return;
            }

            _forumsvc.loadUnreadFriendThreads(MAX_UNREAD_THREADS,
                                              new AsyncCallback<List<ForumThread>>()  {
                public void onSuccess (List<ForumThread> result) {
                    _items = result;
                    for (ForumThread ft : result) {
                        updateThread(ft, UnreadFriendsThreads.this);
                    }
                    doFetchRows(start, count, callback);
                }
                public void onFailure (Throwable failure) {
                    callback.onFailure(failure);
                }
           });
        }

        // from ThreadContainer
        public void registerUpdate (ForumThread thread)
        {
            ForumModels.replaceItem(_items, thread);
        }
    }

    /** A data model that provides a particular thread's messages. */
    public class ThreadMessages
        extends MsoyServiceBackedDataModel<ForumMessage, ForumService.MessageResult>
        implements ThreadContainer
    {
        public ThreadMessages (int threadId) {
            _threadId = threadId;
        }

        public ForumThread getThread () {
            return _thread;
        }

        public boolean canPostReply () {
            return _canPostReply;
        }

        public boolean isManager () {
            return _isManager;
        }

        @Override // from ServiceBackedDataModel
        public void appendItem (ForumMessage message) {
            super.appendItem(message);
            // assign the new index and increment posts
            message.messageIndex = _thread.posts++;
            // mark our thread as read up to this message
            _thread.lastReadPostId = message.messageId;
            _thread.lastReadPostIndex = _thread.posts - 1;
        }

        @Override // from ServiceBackedDataModel
        public void removeItem (ForumMessage message) {
            // if we're deleting the last message in this thread...
            if (_thread.mostRecentPostId == message.messageId) {
                // ...locate the new last message and update our thread with its info
                int idx = _pageItems.indexOf(message);
                if (idx > 0) { // it's in the list and not the first item
                    ForumMessage prev = _pageItems.get(idx-1);
                    _thread.mostRecentPostId = prev.messageId;
                    _thread.mostRecentPoster = prev.poster.name;
                    _thread.mostRecentPostTime = prev.created;
                }
            }

            super.removeItem(message);
            _thread.posts--;
        }

        // from ThreadContainer
        public void registerUpdate (ForumThread thread)
        {
            if (_thread.equals(thread)) {
                if (_thread.posts != thread.posts) {
                    reset();
                }
                _thread = thread;
            }
        }

        @Override // from ServiceBackedDataModel
        protected void onSuccess (ForumService.MessageResult result,
                                  AsyncCallback<List<ForumMessage>> callback) {
            // note some bits
            if (result.thread != null) {
                _thread = result.thread;
                updateThread(result.thread, this);
            }
            _canPostReply = result.canPostReply;
            _isManager = result.isManager;

            // let the PagedGrid know that we're good and to render the items
            super.onSuccess(result, callback);

            // finally update our thread's last read post id so that subsequent renders will show
            // messages as having been read
            if (result.messages.size() > 0) {
                int lastReadIndex = result.messages.size()-1;
                int highestPostId = (result.messages.get(lastReadIndex)).messageId;
                if (highestPostId > _thread.lastReadPostId) {
                    _thread.lastReadPostId = highestPostId;
                    _thread.lastReadPostIndex = _pageOffset + lastReadIndex;
                }
            }
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<ForumService.MessageResult> callback)
        {
            _forumsvc.loadMessages(
                _threadId, _thread.lastReadPostId, start, count, needCount, callback);
        }

        @Override // from ServiceBackedDataModel
        protected int getCount (ForumService.MessageResult result) {
            return result.thread.posts;
        }

        @Override // from ServiceBackedDataModel
        protected List<ForumMessage> getRows (ForumService.MessageResult result) {
            return result.messages;
        }

        protected int _threadId;
        protected ForumThread _thread = new ForumThread(); // dummy to make logic easier
        protected boolean _canPostReply, _isManager;
    }

    /**
     * Notifies the cache that a new thread was posted.
     */
    public void newThreadPosted (ForumThread thread)
    {
        // mark this thread as already read
        thread.lastReadPostId = thread.mostRecentPostId;
        thread.lastReadPostIndex = thread.posts - 1;

        // if we already have this model loaded, let it know about the new thread
        GroupThreads gmodel = _gmodels.get(thread.group.getGroupId());
        if (gmodel != null) {
            gmodel.prependItem(thread);
        }
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the threads for the
     * specified group.
     */
    public GroupThreads getGroupThreads (int groupId)
    {
        GroupThreads gmodel = _gmodels.get(groupId);
        if (gmodel == null) {
            _gmodels.put(groupId, gmodel = new GroupThreads(groupId));
        }
        return gmodel;
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the messages for the
     * specified thread.
     */
    public ThreadMessages getThreadMessages (int threadId)
    {
        ThreadMessages tmodel = _tmodels.get(threadId);
        if (tmodel == null) {
            _tmodels.put(threadId, tmodel = new ThreadMessages(threadId));
        }
        return tmodel;
    }

    /**
     * Returns, creating if necessary, the data model that provides all unread threads for the
     * authenticated user.
     */
    public UnreadThreads getUnreadThreads (boolean refresh)
    {
        if (refresh || _unreadModel == null) {
            _unreadModel = new UnreadThreads();
        }
        return _unreadModel;
    }

    /**
     * Returns, creating if necessary, the data model that provides all threads with unread posts
     * by the authenticated user's friends.
     */
    public UnreadFriendsThreads getUnreadFriendsThreads (boolean refresh)
    {
        if (refresh || _unreadFriendsModel == null) {
            _unreadFriendsModel = new UnreadFriendsThreads();
        }
        return _unreadFriendsModel;
    }

    /**
     * Searches a group's threads for a string and invokes a callback when the results are ready.
     */
    public void searchGroupThreads (int groupId, String query,
                                    AsyncCallback<List<ForumThread>> callback)
    {
        if (_search == null || !_search.equals(groupId, query)) {
            _search = new Search(groupId, query);
        }
        _search.execute(callback);
    }

    /**
     * Registers an updated thread from the server with all known containers, omitting the updater.
     */
    protected void updateThread (ForumThread result, ThreadContainer updater)
    {
        registerUpdate(_unreadModel, result, updater);
        registerUpdate(_unreadFriendsModel, result, updater);

        for (GroupThreads gmodel : _gmodels.values()) {
            registerUpdate(gmodel, result, updater);
        }

        for (ThreadMessages tmodel : _tmodels.values()) {
            registerUpdate(tmodel, result, updater);
        }
    }

    protected static void registerUpdate (
        ThreadContainer target, ForumThread updated, ThreadContainer updater)
    {
        if (target != null && target != updater) {
            target.registerUpdate(updated);
        }
    }

    /**
     * Replaces the first occurrence of the an item in a list with the given instance.
     */
    protected static <T> void replaceItem (List<T> items, T item)
    {
        int idx = items.indexOf(item);
        if (idx != -1) {
            items.set(idx, item);
        }
    }

    /**
     * Operations shared amongst all of our data models.
     */
    protected static interface ThreadContainer
    {
        /**
         * Set all contained threads that are equivalent to refer to it directly.
         */
        void registerUpdate (ForumThread thread);
    }

    /**
     * Parameters and results of searching a group's threads or the user's unread threads.
     */
    protected static class Search
    {
        public Search (int groupId, String query) {
            _groupId = groupId;
            _query = query;
        }

        public boolean equals (int groupId, String query) {
            return _query.equals(query) && _groupId == groupId;
        }

        public void execute (final AsyncCallback<List<ForumThread>> callback) {
            if (_result != null) {
                callback.onSuccess(_result);
            }
            doSearch(new AsyncCallback<List<ForumThread>> () {
                public void onSuccess (List<ForumThread> result) {
                    _result = result;
                    callback.onSuccess(result);
                }
                public void onFailure (Throwable cause) {
                    callback.onFailure(cause);
                }
            });
        }

        protected void doSearch (AsyncCallback<List<ForumThread>> callback) {
            if (_groupId == 0) {
                _forumsvc.findMyThreads(_query, MAX_RESULTS, callback);
            } else {
                _forumsvc.findThreads(_groupId, _query, MAX_RESULTS, callback);
            }
        }

        protected int _groupId;
        protected String _query;
        protected List<ForumThread> _result;
    }

    /** A cache of GroupThreads data models. */
    protected HashMap<Integer, GroupThreads> _gmodels = new HashMap<Integer, GroupThreads>();

    /** A cache of ThreadMessages data models. */
    protected HashMap<Integer, ThreadMessages> _tmodels = new HashMap<Integer, ThreadMessages>();

    /** A cached UnreadThreads data model. */
    protected UnreadThreads _unreadModel;

    /** A cached UnreadThreads data model. */
    protected UnreadFriendsThreads _unreadFriendsModel;

    /** A cached search result. */
    protected Search _search;

    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);

    /** The maximum number of unread threads we'll download at once. */
    protected static final int MAX_UNREAD_THREADS = 100;

    /** The maximum number of thread search results. */
    protected static final int MAX_RESULTS = 20;
}
