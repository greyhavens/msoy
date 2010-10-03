//
// $Id: $

package client.adminz.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import com.threerings.gwt.util.PopupCallback;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigService;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;
import com.threerings.msoy.admin.config.gwt.ConfigServiceAsync;

import client.adminz.config.ConfigEditorTab.ConfigAccessor;
import client.ui.StyledTabPanel;
import com.google.gwt.core.client.GWT;

/**
 *
 */
public class ConfigEditorPanel extends StyledTabPanel
    implements ConfigAccessor
{
    public ConfigEditorPanel ()
    {
        _configsvc.getConfig(new PopupCallback<ConfigurationResult>() {
            public void onSuccess (ConfigurationResult result) {
                gotData(result);
            }
        });
    }

    public void submitChanges (final String tabKey, List<ConfigField> modified)
    {
        _configsvc.updateConfiguration(modified, new PopupCallback<ConfigurationResult>() {
            public void onSuccess (ConfigurationResult result) {
                ConfigEditorTab tab = _tabs.get(tabKey);
                if (tab == null) {
                    // buh?
                    return;
                }
                tab.updateTable(result.records.get(tabKey));
            }
        });
    }

    protected void gotData (ConfigurationResult result)
    {
        clear();

        for (Entry<String, List<ConfigField>> tab : result.records.entrySet()) {
            String tabKey = tab.getKey();
            ConfigEditorTab widget = new ConfigEditorTab(this, tabKey, tab.getValue());
            _tabs.put(tabKey, widget);
            add(widget, tabKey);
        }
    }

    protected Map<String, ConfigEditorTab> _tabs = Maps.newHashMap();

    protected static final ConfigServiceAsync _configsvc = GWT.create(ConfigService.class);

}
