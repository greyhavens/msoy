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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FlexInteger;

/**
 *
 */
public class ConfigInt extends ConfigVariable
{
    private int value;
    private boolean isSet;

    public ConfigInt(OptionSpec option)
    {
        super(option);
        this.isSet = false;
    }

    public ConfigInt(OptionSpec option, int value)
    {
        super(option);
        set(value);
    }

    public void set(int value)
    {
        this.value = value;
        this.isSet = true;
    }

    public void set(String value)
    {
        int intVal;

        try {
            intVal = new FlexInteger(value).intValue();
        } catch (NumberFormatException e) {
            throw new BuildException("Not an integer: " + value);
        }

        this.value = intVal;
        this.isSet = true;
    }

    public boolean isSet() { return isSet; }

    public void addToCommandline(Commandline cmdl)
    {
        if (this.isSet) {
            cmdl.createArgument().setValue("-" + spec.getFullName());
            cmdl.createArgument().setValue(String.valueOf(this.value));
        }
    }

} //End of ConfigInt
