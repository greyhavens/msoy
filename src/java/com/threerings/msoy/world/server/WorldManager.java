//
// $Id$

package com.threerings.msoy.world.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.notify.server.NotificationManager;

import static com.threerings.msoy.Log.log;

/**
 * Handles various global world services.
 */
@Singleton @EventThread
public class WorldManager
    implements WorldProvider
{
    @Inject public WorldManager (InvocationManager invmgr)
    {
        // register our bootstrap invocation service
        invmgr.registerDispatcher(new WorldDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    // from interface MemberProvider
    public void getGroupHomeSceneId (final ClientObject caller, final int groupId,
                                     final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getHomeSceneId", listener, "gid", groupId) {
            @Override public void invokePersistent () throws Exception {
                _homeSceneId = _groupRepo.getHomeSceneId(groupId);
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_homeSceneId);
            }
            protected int _homeSceneId;
        });
    }

    // from interface MemberProvider
    public void getHomePageGridItems (
        ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memObj = (MemberObject) caller;
        final MemberExperience[] experiences = new MemberExperience[memObj.experiences.size()];
        memObj.experiences.toArray(experiences);
        final boolean onTour = memObj.onTour;
        final int memberId = memObj.getMemberId();
        final short badgesVersion = memObj.getLocal(MemberLocal.class).badgesVersion;

        _invoker.postUnit(new PersistingUnit("getHPGridItems", listener, "who", memObj.who()) {
            @Override public void invokePersistent () throws Exception {
                _result = _memberLogic.getHomePageGridItems(
                    memberId, experiences, onTour, badgesVersion);
            }

            @Override public void handleSuccess () {
                reportRequestProcessed(_result);
            }

            protected Object _result;
        });
    }

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberLogic _memberLogic;
}
