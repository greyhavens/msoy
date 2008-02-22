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
package flex.ant.types;

import flex.ant.config.OptionSource;
import flex.ant.config.OptionSpec;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FlexInteger;

/**
 *
 */
public class DefaultScriptLimits implements DynamicAttribute, OptionSource
{
    public static OptionSpec spec = new OptionSpec("default-script-limits");

    private int rec = -1;
    private int exe = -1;

    public void setDynamicAttribute(String name, String value)
    {
        int intVal = new FlexInteger(value).intValue();

        if (name.equals("max-recursion-depth"))
            rec = intVal;
        else if (name.equals("max-execution-time"))
            exe = intVal;
        else
            throw new BuildException("The <default-script-limits> type doesn't support the \""
                    + name + "\" attribute.");

        if (intVal < 0)
            throw new BuildException(name + "attribute must be a positive integer!");
    }

    public void addToCommandline(Commandline cmdl)
    {
        if (rec == -1)
            throw new BuildException("max-recursion-depth attribute must be set!");
        else if (exe == -1)
            throw new BuildException("max-execution-time attribute must be set!");
        else {
            cmdl.createArgument().setValue("-" + spec.getFullName());
            cmdl.createArgument().setValue(String.valueOf(rec));
            cmdl.createArgument().setValue(String.valueOf(exe));
        }
    }

} //End of DefaultScriptLimits
