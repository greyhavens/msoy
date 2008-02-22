/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package flex.ant.config;

import org.apache.tools.ant.types.Commandline;

/**
 *
 */
public final class ConfigBoolean extends ConfigVariable
{
    private boolean enabled;
    private boolean isSet;

    public ConfigBoolean(OptionSpec spec)
    {
        super(spec);

        this.enabled = false;
        this.isSet = false;
    }

    public ConfigBoolean(OptionSpec spec, boolean enabled)
    {
        super(spec);
        this.set(enabled);
    }

    public void set(boolean value)
    {
        this.enabled = value;
        this.isSet = true;
    }

    public void set(String value)
    {
        this.enabled = parseValue(value);
        this.isSet = true;
    }

    public boolean isSet() { return isSet; }

    public void addToCommandline(Commandline cmdl)
    {
        if (isSet)
            cmdl.createArgument(true).setValue("-" + spec.getFullName() + "=" + enabled);
    }

    private boolean parseValue(String value)
    {
        return value.toLowerCase().matches("\\s*(true|yes|on)\\s*");
    }
    
} //End of ConfigBoolean
