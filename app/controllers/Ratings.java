package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;

/**
 * Created by stefan on 6/16/17.
 */
public class Ratings extends Controller {

    public static Result visitInfo(){
        JsonNode ajax_json = request().body().asJson();
        return ok(ajax_json.toString());
    }

    public static Result rateRestaurantAJAX() {
        try (Connection connection = DB.getConnection()) {
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
