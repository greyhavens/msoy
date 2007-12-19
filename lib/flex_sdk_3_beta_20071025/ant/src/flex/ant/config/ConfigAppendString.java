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
public class ConfigAppendString extends ConfigString
{
    public ConfigAppendString(OptionSpec option)
    {
        super(option);
    }

    public ConfigAppendString(OptionSpec option, String value)
    {
        super(option, value);
    }

    public void addToCommandline(Commandline cmdl)
    {
        String value = value();

        if ((value != null) && (value.length() > 0))
        {
            cmdl.createArgument().setValue("+" + spec.getFullName() + "=" + value);
        }
    }
}
