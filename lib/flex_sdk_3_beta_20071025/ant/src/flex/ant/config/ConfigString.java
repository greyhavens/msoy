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
public class ConfigString extends ConfigVariable
{
    private String value;

    public ConfigString(OptionSpec option)
    {
        this(option, null);
    }

    public ConfigString(OptionSpec option, String value)
    {
        super(option);
        this.set(value);
    }
    
    public void set(String value)
    {
        this.value = value;
    }

    public boolean isSet() { return value != null; }

    public String value() { return value; }

    public void addToCommandline(Commandline cmdl)
    {
        if (value != null)
        {
            if (value.length() > 0)
            {
                cmdl.createArgument().setValue("-" + spec.getFullName() + "=" + value);
            }
            else
            {
                cmdl.createArgument().setValue("-" + spec.getFullName() + "=");
            }
        }
    }

} //End of ConfigurationString
