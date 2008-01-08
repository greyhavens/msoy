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

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryService
{

    public List getCategories()
    {

        ArrayList list = new ArrayList();
        Connection c=null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM restaurant_category ORDER BY name");
            while (rs.next())
            {
                list.add(new RestaurantCategory(
                rs.getInt("category_id"),
                rs.getString("name")));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                c.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        return list;

    }

}
