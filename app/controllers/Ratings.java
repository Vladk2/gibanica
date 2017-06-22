package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import models.Restaurant;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.visits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stefan on 6/16/17.
 */
public class Ratings extends Controller {

    public static Result visits() {
        if(session("userType").equals("guest")) {
            Calendar currentTime = Calendar.getInstance();
            System.out.println(currentTime.getTime());

            Calendar startTime = Calendar.getInstance();

            Calendar endTime = Calendar.getInstance();



            try (Connection connection = DB.getConnection()) {
                /* PORED PODATAKA O RESTARANU, TREBA DA SE IZLISTAJU I PODACI O POSETI (to jos nije uradjeno) */
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "Select r.restaurantId, r.name, r.description, r.address, r.tel, r.size from restaurants as r " +
                                "left join visits as v on r.restaurantId = v.restaurantId left join users as u " +
                                "on v.userId = u.userId where u.email = (Select email from users where email = ?);");

                preparedStatement.setString(1, session("connected"));

                ResultSet resultSet = preparedStatement.executeQuery();

                List<Restaurant> restaurants = new ArrayList<Restaurant>();

                while (resultSet.next()) {
                    restaurants.add(new Restaurant(
                            resultSet.getString(1),
                            resultSet.getString(2),
                            resultSet.getString(3),
                            resultSet.getString(4),
                            resultSet.getString(5),
                            resultSet.getString(6)
                    ));
                }

                resultSet.close();
                preparedStatement.close();

                /* uzimanje ocena  */

                String query_ratings = "Select * from restaurantsRating where userId = ?";

                PreparedStatement preparedStatement1 = connection.prepareStatement(query_ratings);
                preparedStatement1.setString(1, session("userId"));

                ResultSet resultSet1 = preparedStatement1.executeQuery();

                List<HashMap<String, String>> ratings = new ArrayList<>();

                while(resultSet1.next()){
                    HashMap<String, String> rating = new HashMap<>();
                    rating.put("userId", resultSet1.getString(1));
                    rating.put("restaurantId", resultSet1.getString(2));
                    rating.put("visitId", resultSet1.getString(3));
                    rating.put("rating", resultSet1.getString(4));
                    rating.put("serviceRating", resultSet1.getString(5));
                    rating.put("mealRating", resultSet1.getString(6));
                    ratings.add(rating);
                }

                resultSet1.close();
                preparedStatement1.close();

                ObjectMapper objectMapper = new ObjectMapper();
                String for_js_ratings = objectMapper.writeValueAsString(ratings);

                return ok(visits.render(session("connected"), restaurants, ratings, for_js_ratings));
            } catch (SQLException e) {
                e.printStackTrace();
                return badRequest("Something strange happened");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return forbidden();
    }

    @SuppressWarnings("Duplicates")
    public static Result rateFoodAJAX() {
        try (Connection connection = DB.getConnection()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> response = objectMapper.convertValue(ajax_json, HashMap.class);

            /* update table restaurantsRating (userId, restaurantId, visitId, ratingFood) iz requesta */

            connection.setAutoCommit(false);

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "Update restaurantsRating set mealRating = ? where userId = (Select userId " +
                            "from users where email = ?) and restaurantId = ? and " +
                            "visitId = ?;"
            );

            preparedStatement.setInt(1, Integer.parseInt(response.get("food_rating")));
            preparedStatement.setString(2, response.get("userId"));
            preparedStatement.setInt(3, Integer.parseInt(response.get("restaurantId")));
            preparedStatement.setInt(4,Integer.parseInt(response.get("visitId")));

            preparedStatement.execute();

            connection.commit();


            return ok(ajax_json.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return badRequest("Something strange happened");
        }
    }

    @SuppressWarnings("Duplicates")
    public static Result rateServiceAJAX() {
        try (Connection connection = DB.getConnection()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> response = objectMapper.convertValue(ajax_json, HashMap.class);

            /* update table restaurantsRating (userId, restaurantId, visitId, ratingFood) iz requesta */

            connection.setAutoCommit(false);

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "Update restaurantsRating set serviceRating = ? where userId = (Select userId " +
                            "from users where email = ?) and restaurantId = ? and " +
                            "visitId = ?"
            );

            preparedStatement.setInt(1, Integer.parseInt(response.get("service_rating")));
            preparedStatement.setString(2, response.get("userId"));
            preparedStatement.setInt(3, Integer.parseInt(response.get("restaurantId")));
            preparedStatement.setInt(4,Integer.parseInt(response.get("visitId")));

            preparedStatement.execute();

            connection.commit();

            return ok(ajax_json.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return badRequest("Something strange happened");
        }
    }

    public static Result rateRestaurantAJAX() {
        try (Connection connection = DB.getConnection()) {
            connection.setAutoCommit(false);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> response = objectMapper.convertValue(ajax_json, HashMap.class);
            System.out.println(response.toString());

            int restaurant_id =
                    Integer.parseInt(response.get("form_id"));

            System.out.println(restaurant_id);

            int mark_final = Integer.parseInt(response.get("rating"));

            System.out.println(mark_final);

            // form_id je u sustini id restorana iz baze. prilikom prikaza na html stranici
            // formi ce biti dodeljen taj id + 'rst' -> restaurant

            PreparedStatement statement = connection.prepareStatement("Insert into restaurantsRating (userId, restaurantId, visitId, rating) values " +
                    "(?, ?, ?, ?);");

            statement.setInt(1, Integer.parseInt(session("userId")));
            statement.setInt(2, restaurant_id);
            statement.setInt(3, 1);
            statement.setInt(4, mark_final);

            statement.execute();

            connection.commit();

            return ok();

        } catch(MySQLIntegrityConstraintViolationException duplicate_key){
            duplicate_key.printStackTrace();
            return badRequest("Already rated");
        } catch (Exception e){
            e.printStackTrace();
            return badRequest("Something strange happened");
        }
    }
}
