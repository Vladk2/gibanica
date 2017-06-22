package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.FriendsSearchData;
import models.Restaurant;
import models.RestaurantSearchData;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by goran on 21.6.17..
 */
public class Reservations extends Controller {

    public static Result searchRestaurants() throws SQLException{

        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }


        JsonNode jsonReq = request().body().asJson();
        RestaurantSearchData restaurantSearchData = Json.fromJson(jsonReq, RestaurantSearchData.class);

        Connection connection = null;
        PreparedStatement searchQuery = null;
        ResultSet resultSet = null;
        ArrayList<Restaurant> resultArray = new ArrayList<Restaurant>();

        try {
            connection = DB.getConnection();
            searchQuery = connection.prepareStatement("select * from baklava.restaurants " +
                    "where name like ? or description like ?");
            searchQuery.setString(1, "%" + restaurantSearchData.text + "%");
            searchQuery.setString(2, "%" + restaurantSearchData.text + "%");

            resultSet = searchQuery.executeQuery();

            while (resultSet.next()) {
                resultArray.add(new Restaurant(
                        Integer.toString(resultSet.getInt(1)),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        Integer.toString(resultSet.getInt(6))
                ));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (searchQuery != null) {
                searchQuery.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return ok(Json.toJson(resultArray));
    }

    public static Result reserve(int id) {
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }


        return badRequest(Integer.toString(id));
    }

}
