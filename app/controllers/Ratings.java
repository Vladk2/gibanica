package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.visits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by stefan on 6/16/17.
 */
public class Ratings extends Controller {

    public static Result visits() {
        try (Connection connection = DB.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "Select r.restaurantId, r.name, r.address, r.description from restaurants as r " +
                            "left join visits as v on r.restaurantId = v.restaurantId left join users as u " +
                            "on v.userId = u.userId where u.email = (Select email from users where email = ?);");

            preparedStatement.setString(1, session("connected"));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                System.out.println(resultSet.getString(1));
                System.out.println(resultSet.getString(2));
                System.out.println(resultSet.getString(3));
                System.out.println(resultSet.getString(4));
            }


            return ok(visits.render(session("connected")));
        } catch (SQLException e) {
            e.printStackTrace();
            return badRequest("Nesto se cudno desilo");
        }
    }

    public static Result visitInfo(){
        JsonNode ajax_json = request().body().asJson();
        return ok(ajax_json.toString());
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
            return badRequest("Vec ste ocenili ovaj restoran.");
        } catch (Exception e){
            e.printStackTrace();
            return badRequest("Desilo se nesto cudno.");
        }
    }
}
