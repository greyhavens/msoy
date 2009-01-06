//
// $Id$

package com.threerings.msoy.admin.data;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.JPanel;

import com.threerings.presents.util.PresentsContext;

import com.threerings.admin.client.AsStringFieldEditor;
import com.threerings.admin.data.ConfigObject;

import com.threerings.msoy.admin.util.AdminContext;

/**
 * Contains runtime configurable general server configuration.
 */
public class ServerConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>nonAdminsAllowed</code> field. */
    public static final String NON_ADMINS_ALLOWED = "nonAdminsAllowed";

    /** The field name of the <code>registrationEnabled</code> field. */
    public static final String REGISTRATION_ENABLED = "registrationEnabled";

    /** The field name of the <code>humanityReassessment</code> field. */
    public static final String HUMANITY_REASSESSMENT = "humanityReassessment";

    /** The field name of the <code>nextReboot</code> field. */
    public static final String NEXT_REBOOT = "nextReboot";

    /** The field name of the <code>customRebootMsg</code> field. */
    public static final String CUSTOM_REBOOT_MSG = "customRebootMsg";

    /** The field name of the <code>servletRebootInitiator</code> field. */
    public static final String SERVLET_REBOOT_INITIATOR = "servletRebootInitiator";

    /** The field name of the <code>servletReboot</code> field. */
    public static final String SERVLET_REBOOT = "servletReboot";

    /** The field name of the <code>servletRebootNode</code> field. */
    public static final String SERVLET_REBOOT_NODE = "servletRebootNode";
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

    /** Whether or not to allow new registrations. */
    public boolean registrationEnabled = true;

    /** The number of seconds between reassessments of a member's humanity factor. */
    public int humanityReassessment = 24 * 3600;

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
    public void setRegistrationEnabled (boolean value)
    {
        boolean ovalue = this.registrationEnabled;
        requestAttributeChange(
            REGISTRATION_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.registrationEnabled = value;
    }

    /**
     * Requests that the <code>humanityReassessment</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHumanityReassessment (int value)
    {
        int ovalue = this.humanityReassessment;
        requestAttributeChange(
            HUMANITY_REASSESSMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.humanityReassessment = value;
    }

    /**
     * Requests that the <code>nextReboot</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
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
    public void setServletRebootNode (String value)
    {
        String ovalue = this.servletRebootNode;
        requestAttributeChange(
            SERVLET_REBOOT_NODE, value, ovalue);
        this.servletRebootNode = value;
    }
    // AUTO-GENERATED: METHODS END
}
