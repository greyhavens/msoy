//
// $Id$

package com.threerings.msoy.apps.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.server.persist.AppInfoRecord;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Implements the application service.
 */
public class AppServlet extends MsoyServiceServlet
    implements AppService
{
    @Override // from AppService
    public List<AppInfo> getApps ()
        throws ServiceException
    {
        requireAdminUser();
        return Lists.newArrayList(Lists.transform(_appRepo.loadApps(), AppInfoRecord.TO_APP_INFO));
    }

    @Override // from AppService
    public int createApp (String name)
        throws ServiceException
    {
        requireAdminUser();
        AppInfoRecord app = new AppInfoRecord();
        app.name = name;
        _appRepo.createApp(app);
        return app.appId;
    }

    @Override // from AppService
    public AppData getAppData (int appId)
        throws ServiceException
    {
        requireAdminUser();
        AppData data = new AppData();
        data.info = requireApp(appId).toAppInfo();
        data.facebook = _facebookRepo.loadAppFacebookInfo(appId);
        return data;
    }

    @Override // from AppService
    public void deleteApp (int appId)
        throws ServiceException
    {
        requireAdminUser();
        _appRepo.deleteApp(requireApp(appId).appId);
    }

    @Override // from AppService
    public void updateAppInfo (AppInfo info)
        throws ServiceException
    {
        requireAdminUser();
        AppInfoRecord updated = requireApp(info.appId);
        updated.update(info);
        _appRepo.updateAppInfo(updated);
    }

    @Override // from AppService
    public void updateFacebookInfo (FacebookInfo info)
        throws ServiceException
    {
        requireAdminUser();
        _facebookRepo.updateFacebookInfo(info);
    }

    protected AppInfoRecord requireApp (int id)
        throws ServiceException
    {
        requireAdminUser();
        AppInfoRecord app = _appRepo.loadAppInfo(id);
        if (app == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        return app;
    }

    // dependencies
    @Inject AppRepository _appRepo;
    @Inject FacebookRepository _facebookRepo;
}
