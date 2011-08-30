//
// $Id$

package com.threerings.msoy.admin.data;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Date;

import javax.annotation.Generated;
import javax.swing.JPanel;

import com.threerings.presents.util.PresentsContext;

import com.threerings.admin.client.AsStringFieldEditor;
import com.threerings.admin.data.ConfigObject;

import com.threerings.msoy.admin.util.AdminContext;

/**
 * Contains runtime configurable general server configuration.
 */
@com.threerings.util.ActionScript(omit=true)
public class ServerConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>nonAdminsAllowed</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NON_ADMINS_ALLOWED = "nonAdminsAllowed";

    /** The field name of the <code>registrationEnabled</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String REGISTRATION_ENABLED = "registrationEnabled";

    /** The field name of the <code>nextReboot</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEXT_REBOOT = "nextReboot";

    /** The field name of the <code>customRebootMsg</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String CUSTOM_REBOOT_MSG = "customRebootMsg";

    /** The field name of the <code>servletRebootInitiator</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String SERVLET_REBOOT_INITIATOR = "servletRebootInitiator";

    /** The field name of the <code>servletReboot</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String SERVLET_REBOOT = "servletReboot";

    /** The field name of the <code>servletRebootNode</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String SERVLET_REBOOT_NODE = "servletRebootNode";

    /** The field name of the <code>maxInvokerQueueSize</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MAX_INVOKER_QUEUE_SIZE = "maxInvokerQueueSize";

    /** The field name of the <code>maxPendingClientResolutions</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MAX_PENDING_CLIENT_RESOLUTIONS = "maxPendingClientResolutions";

    /** The field name of the <code>fbNotificationsAlloc</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String FB_NOTIFICATIONS_ALLOC = "fbNotificationsAlloc";
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

    /** Whether or not to allow new registrations. */
    public boolean registrationEnabled = true;

    /** The time at which the next reboot will occur. */
    public long nextReboot;

    /** A custom reboot message input by an admin. */
    public String customRebootMsg;

    /** A custom initiator if the attribute is changed by the admin servlet. */
    public String servletRebootInitiator;

    /** The time a servlet reboot was scheduled, determines if nextReboot was set by a servlet. */
    public long servletReboot;

    /** Node on which a servlet reboot was issued (needed to avoid sending multiple mails). */
    public String servletRebootNode;

    /** Authentications are disabled once the the invoker queue reaches this size. */
    public int maxInvokerQueueSize = 100;

    /** Authentications are disabled if there are more than this many pending logins. */
    public int maxPendingClientResolutions = 20;

    /** Number of allocations we are allowed by facebook for user-to-user notifications.
     * TODO: remove when we figure out how to get that dynamically
     */
    public int fbNotificationsAlloc = 20;

    @Override // documentation inherited
    public JPanel getEditor (PresentsContext ctx, Field field)
    {
        String name = field.getName();
        if (NEXT_REBOOT.equals(name)) {
            final DateFormat dfmt = DateFormat.getDateTimeInstance(
                DateFormat.LONG, DateFormat.SHORT,
                ((AdminContext) ctx).getMessageManager().getLocale());
            return new AsStringFieldEditor(ctx, field, this) {
                protected void displayValue (Object value) {
                    _value.setText(dfmt.format(new Date(nextReboot)));
                }
                protected Object getDisplayValue () throws Exception {
                    try {
                        return Long.valueOf(dfmt.parse(_value.getText()).getTime());
                    } catch (Exception e) {
                    }
                    try {
                        return System.currentTimeMillis() +
                            (60*1000L) * Long.parseLong(_value.getText());
                    } catch (Exception e) {
                        return 0L;
                    }
                }
            };

        } else {
            return super.getEditor(ctx, field);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>nonAdminsAllowed</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setNonAdminsAllowed (boolean value)
    {
        boolean ovalue = this.nonAdminsAllowed;
        requestAttributeChange(
            NON_ADMINS_ALLOWED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.nonAdminsAllowed = value;
    }

    /**
     * Requests that the <code>registrationEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setRegistrationEnabled (boolean value)
    {
        boolean ovalue = this.registrationEnabled;
        requestAttributeChange(
            REGISTRATION_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.registrationEnabled = value;
    }

    /**
     * Requests that the <code>nextReboot</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setNextReboot (long value)
    {
        long ovalue = this.nextReboot;
        requestAttributeChange(
            NEXT_REBOOT, Long.valueOf(value), Long.valueOf(ovalue));
        this.nextReboot = value;
    }

    /**
     * Requests that the <code>customRebootMsg</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCustomRebootMsg (String value)
    {
        String ovalue = this.customRebootMsg;
        requestAttributeChange(
            CUSTOM_REBOOT_MSG, value, ovalue);
        this.customRebootMsg = value;
    }

    /**
     * Requests that the <code>servletRebootInitiator</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setServletRebootInitiator (String value)
    {
        String ovalue = this.servletRebootInitiator;
        requestAttributeChange(
            SERVLET_REBOOT_INITIATOR, value, ovalue);
        this.servletRebootInitiator = value;
    }

    /**
     * Requests that the <code>servletReboot</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setServletReboot (long value)
    {
        long ovalue = this.servletReboot;
        requestAttributeChange(
            SERVLET_REBOOT, Long.valueOf(value), Long.valueOf(ovalue));
        this.servletReboot = value;
    }

    /**
     * Requests that the <code>servletRebootNode</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setServletRebootNode (String value)
    {
        String ovalue = this.servletRebootNode;
        requestAttributeChange(
            SERVLET_REBOOT_NODE, value, ovalue);
        this.servletRebootNode = value;
    }

    /**
     * Requests that the <code>maxInvokerQueueSize</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMaxInvokerQueueSize (int value)
    {
        int ovalue = this.maxInvokerQueueSize;
        requestAttributeChange(
            MAX_INVOKER_QUEUE_SIZE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.maxInvokerQueueSize = value;
    }

    /**
     * Requests that the <code>maxPendingClientResolutions</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMaxPendingClientResolutions (int value)
    {
        int ovalue = this.maxPendingClientResolutions;
        requestAttributeChange(
            MAX_PENDING_CLIENT_RESOLUTIONS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.maxPendingClientResolutions = value;
    }

    /**
     * Requests that the <code>fbNotificationsAlloc</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setFbNotificationsAlloc (int value)
    {
        int ovalue = this.fbNotificationsAlloc;
        requestAttributeChange(
            FB_NOTIFICATIONS_ALLOC, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.fbNotificationsAlloc = value;
    }
    // AUTO-GENERATED: METHODS END
}
