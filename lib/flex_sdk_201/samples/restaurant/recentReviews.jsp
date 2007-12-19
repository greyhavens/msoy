<%@ page language="java" contentType="text/xml; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="samples.restaurant.*" %>
<%@ page import="java.util.List" %>
<%
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
%>

<list>

<%
	RestaurantService service = new RestaurantService();
	List restaurantReviews = service.getRecentReviews();
	for (int i = 0; i < restaurantReviews.size(); i++)
	{
		Review review = (Review) restaurantReviews.get(i);
%>
	<review>
		<reviewer><%= review.getReviewer() %></reviewer>
		<title><%= review.getTitle() %></title>
		<reviewText><%= review.getReviewText() %></reviewText>
		<reviewDate><%= review.getReviewDate() %></reviewDate>
		<rating><%= review.getRating() %></rating>
		<restaurantId><%= review.getRestaurant().getRestaurantId() %></restaurantId>
		<restaurantName><%= review.getRestaurant().getName() %></restaurantName>
		<image><%= review.getRestaurant().getImage() %></image>
    </review>
<%
	}
%>

</list>