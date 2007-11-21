//
// $Id$

package client.msgs;

import java.util.List;

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
            super.onSuccess(result);
            _canStartThread = ((ForumService.ThreadResult)result).canStartThread;
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
}
