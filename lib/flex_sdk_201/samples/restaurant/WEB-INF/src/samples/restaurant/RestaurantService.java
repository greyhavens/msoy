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

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class RestaurantService
{

    public List getRestaurants()
    {
        ArrayList list = new ArrayList();
        Connection c=null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT restaurant_id, name, city FROM restaurant ORDER BY name");
            while (rs.next())
            {
                list.add(new RestaurantSummary(rs.getInt("restaurant_id"), rs.getString("name"), rs.getString("city")));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return list;

    }

    public List getRestaurantsByCategories(int[] categories)
    {

        StringBuffer sb = new StringBuffer("(");
        for (int i=0; i<categories.length; i++)
        {
            sb.append(categories[i] +",");
        }
        sb.setCharAt(sb.length()-1, ')');

        ArrayList list = new ArrayList();

        Connection c=null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT r.restaurant_id, name, city FROM restaurant AS r, restaurant_restaurant_category AS c WHERE r.restaurant_id=c.restaurant_id AND category_id IN "+sb+" GROUP BY restaurant_id, name, city, phone ORDER BY name");
            while (rs.next())
            {
                list.add(new RestaurantSummary(rs.getInt("restaurant_id"), rs.getString("name"), rs.getString("city")));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return list;

    }


    public Restaurant getRestaurant(int restaurantId)
    {

        Restaurant restaurant = null;
        Connection c = null;

        try
        {
            c = ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM restaurant WHERE restaurant_id=" + restaurantId);
            if (rs.next())
            {
                restaurant = new Restaurant();
                restaurant.setRestaurantId(rs.getInt("restaurant_id"));
                restaurant.setName(rs.getString("name"));
                restaurant.setAddress(rs.getString("address"));
                restaurant.setCity(rs.getString("city"));
                restaurant.setZip(rs.getString("zip"));
                restaurant.setPhone(rs.getString("phone"));
                restaurant.setLink(rs.getString("link"));
                restaurant.setImage(rs.getString("image"));
                restaurant.setDescription(rs.getString("description"));
                restaurant.setCategories(getCategories(restaurantId));

                rs = s.executeQuery("select avg(rating) as rating FROM restaurant_review WHERE restaurant_id=" + restaurantId);

                if (rs.next())
                {
                    restaurant.setRating(rs.getInt("rating"));
                }

            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return restaurant;

    }

    public int getRating(int restaurantId)
    {

        Connection c = null;

        int rating = -1;

        try
        {
            c = ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("select avg(rating) as rating FROM restaurant_review WHERE restaurant_id=" + restaurantId);

            if (rs.next())
            {
                rating = rs.getInt("rating");
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return rating;

    }

    public List getCategories(int restaurantId)
    {

        ArrayList list = new ArrayList();
        Connection c=null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT a.category_id, b.name FROM restaurant_restaurant_category AS a, restaurant_category AS b WHERE a.category_id=b.category_id AND a.restaurant_id="+restaurantId);
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
            ConnectionManager.closeConnection(c);
        }
        return list;
    }

    public List getReviews(int restaurantId)
    {

        ArrayList list = new ArrayList();

        Connection c = null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM restaurant_review WHERE restaurant_id="+restaurantId+" ORDER BY review_date DESC");
            while (rs.next())
            {
                list.add(new Review(
                rs.getTimestamp("review_date"),
                rs.getString("reviewer"),
                rs.getInt("rating"),
                rs.getString("title"),
                rs.getString("review_text")));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return list;
    }

    public List getRecentReviews()
    {
        ArrayList list = new ArrayList();

        Connection c = null;

        try
        {
            c=ConnectionManager.getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT LIMIT 0 10 * FROM restaurant_review ORDER BY review_date DESC");
            while (rs.next())
            {
                Review review = new Review(
                rs.getTimestamp("review_date"),
                rs.getString("reviewer"),
                rs.getInt("rating"),
                rs.getString("title"),
                rs.getString("review_text"));
                review.setRestaurant(getRestaurant(rs.getInt("restaurant_id")));
                list.add(review);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
        return list;
    }


    public void addReview(Review review)
    {
        Connection c = null;
        try
        {
            c=ConnectionManager.getConnection();
            PreparedStatement ps = c.prepareStatement("INSERT INTO restaurant_review (restaurant_id, reviewer, rating, title, review_text) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, review.getRestaurantId());
            ps.setString(2, review.getReviewer());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getTitle());
            ps.setString(5, review.getReviewText());
            ps.execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            ConnectionManager.closeConnection(c);
        }
    }
}