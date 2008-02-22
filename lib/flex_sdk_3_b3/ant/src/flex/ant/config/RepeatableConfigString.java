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

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class RepeatableConfigString extends RepeatableConfigVariable
{
    private final ArrayList values;
    
    public RepeatableConfigString(OptionSpec spec)
    {
        super(spec);

        values = new ArrayList();
    }

    public void add(String value)
    {
        values.add(value);
    }

    public void addToCommandline(Commandline cmdl)
    {
       if (values.size() != 0)
            cmdl.createArgument().setValue("-" + spec.getFullName() + "=" + makeArgString());
    }

    private String makeArgString()
    {
        String arg = "";
        Iterator it = values.iterator();

        while (it.hasNext()) {
            arg += (String) it.next();
            arg += it.hasNext() ? "," : "";
        }

        return arg;
    }

} //End of RepeatableConfigString
