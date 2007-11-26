//
// $Id$

package client.msgs;

import java.util.List;

import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.web.client.ForumService;

import client.util.ServiceBackedDataModel;

/**
 * Various data models used by hte forum services.
 */
public class ForumModels
{
    /** A data model that provides a particular group's threads. */
    public static class GroupThreads extends ServiceBackedDataModel
    {
        public GroupThreads (int groupId) {
            _groupId = groupId;
        }

        public boolean canStartThread () {
            return _canStartThread;
        }

        public void onSuccess (Object result) {
            _canStartThread = ((ForumService.ThreadResult)result).canStartThread;
            super.onSuccess(result);
        }

        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.forumsvc.loadThreads(CMsgs.ident, _groupId, start, count, needCount, this); 
        }
        protected int getCount (Object result) {
            return ((ForumService.ThreadResult)result).threadCount;
        }
        protected List getRows (Object result) {
            return ((ForumService.ThreadResult)result).threads;
        }

        protected int _groupId;
        protected boolean _canStartThread;
    }

    /** A data model that provides a particular thread's messages. */
    public static class ThreadMessages extends ServiceBackedDataModel
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

        public void onSuccess (Object result) {
            ForumService.MessageResult mresult = (ForumService.MessageResult)result;
            if (mresult.thread != null) {
                _thread = mresult.thread;
            }
            _canPostReply = mresult.canPostReply;
            super.onSuccess(result);
        }

        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.forumsvc.loadMessages(CMsgs.ident, _threadId, start, count, needCount, this); 
        }
        protected int getCount (Object result) {
            return ((ForumService.MessageResult)result).thread.posts;
        }
        protected List getRows (Object result) {
            return ((ForumService.MessageResult)result).messages;
        }

        protected int _threadId;
        protected ForumThread _thread;
        protected boolean _canPostReply;
    }
}
