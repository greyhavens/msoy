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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.types.Commandline;

public class URLElement implements DynamicAttribute, OptionSource
{
    private static final String RSL_URL = "rsl-url";
    private static final String POLICY_FILE_URL = "policy-file-url";

    private String rslURL;
    private String policyFileURL;

    public void setDynamicAttribute(String name, String value)
    {
        if (name.equals(RSL_URL))
        {
            rslURL = value;
        }
        else if (name.equals(POLICY_FILE_URL))
        {
            policyFileURL = value;
        }
        else
        {
            throw new BuildException("The <url> type doesn't support the \"" +
                                     name + "\" attribute.");            
        }
    }

    public void addToCommandline(Commandline commandLine)
    {
        if (rslURL != null)
        {
            commandLine.createArgument().setValue(rslURL);
        }
        
        if (policyFileURL != null)
        {
            commandLine.createArgument().setValue(policyFileURL);
        }
    }
}