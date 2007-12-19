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

/**
 *
 */
public class OptionSpec
{
    private String prefix;
    private String name;
    private String alias;

    /**
     *
     */
    public OptionSpec(String name)
    {
        this.name = name;
    }

    /**
     *
     */
    public OptionSpec(String prefix, String name)
    {
        this.prefix = prefix;
        this.name = name;
    }

    /**
     *
     */
    public OptionSpec(String prefix, String name, String alias)
    {
        this.prefix = prefix;
        this.name = name;
        this.alias = alias;
    }

    /**
     *
     */
    public String getFullName()
    {
        String result;

        if (prefix != null)
        {
            result = prefix + "." + name;
        }
        else
        {
            result = name;
        }

        return result;
    }

    /**
     *
     */
    public String getName()
    {
        return name;
    }

    /**
     *
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     *
     */
    public String getAlias()
    {
        return alias;
    }

    /**
     *
     */
    public boolean matches(String option)
    {
        boolean result = false;

        if ((prefix != null) && option.equals(prefix + "." + name))
        {
            result = true;
        }
        else if (option.equals(name))
        {
            result = true;
        }
        else if ((alias != null) && option.equals(alias))
        {
            result = true;
        }

        return result;
    }

} //End of OptionSpec
