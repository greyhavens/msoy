////////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
// All Rights Reserved.
// The following is Sample Code and is subject to all restrictions on such code
// as contained in the End User License Agreement accompanying this product.
// If you have received this file from a source other than Adobe,
// then your use, modification, or distribution of it requires
// the prior written permission of Adobe.
//
////////////////////////////////////////////////////////////////////////////////
package samples.restaurant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.net.URLDecoder;

public class ConnectionManager
{

    private String url;

    private static ConnectionManager instance;

    public String getUrl()
    {
        return url;
    }

    private ConnectionManager()
    {
        try
        {
            Class.forName("org.hsqldb.jdbcDriver");
            // Obtain a path to WEB-INF/classes/restaurant
            // There seemed to be a conflict getting a resource named "restaurant"
            String str = URLDecoder.decode(getClass().getClassLoader().getResource("samples/restaurant/ConnectionManager.class").toString(),"UTF-8");
            // Create HSQLDB JDBC URL pointing to WEB-INF/restaurant (where the last restaurant is the datanase name)
            url = "jdbc:hsqldb:" + str.substring(0, str.indexOf("classes/samples/restaurant")) + "db/restaurant/restaurant";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static ConnectionManager getInstance()
    {
        if (instance == null)
            instance = new ConnectionManager();
        return instance;
    }

    public static Connection getConnection() throws java.sql.SQLException
    {
        Connection connection =  DriverManager.getConnection(getInstance().getUrl());
        return connection;
    }

    public static void closeConnection(Connection c)
    {
        try
        {
            if (c != null)
            {
                c.close();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

}