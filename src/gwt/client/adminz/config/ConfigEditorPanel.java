//
// $Id: $

package client.adminz.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PopupCallback;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigService;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;
import com.threerings.msoy.admin.config.gwt.ConfigServiceAsync;

import client.ui.StyledTabPanel;

import client.adminz.config.ConfigEditorTab.ConfigAccessor;

/**
 *
 */
public class ConfigEditorPanel extends StyledTabPanel
    implements ConfigAccessor
{
    public ConfigEditorPanel ()
    {
        addStyleName("configEditorPanel");
        _configsvc.getConfig(new PopupCallback<ConfigurationResult>() {
            public void onSuccess (ConfigurationResult result) {
                gotData(result);
            }
        });
    }

    public void submitChanges (String key, List<ConfigField> modified,
                               AsyncCallback<ConfigurationResult> callback)
    {
        _configsvc.updateConfiguration(key, modified, callback);
    }

    protected void gotData (ConfigurationResult result)
    {
        clear();

        if (result.records.isEmpty()) {
            return;
        }

        for (Entry<String, List<ConfigField>> tab : result.records.entrySet()) {
            String tabKey = tab.getKey();
            ConfigEditorTab widget = new ConfigEditorTab(this, tabKey, tab.getValue());
            _tabs.put(tabKey, widget);
            add(widget, tabKey);
        }
        selectTab(0);
    }

    protected Map<String, ConfigEditorTab> _tabs = Maps.newHashMap();

    protected static final ConfigServiceAsync _configsvc = GWT.create(ConfigService.class);
}
