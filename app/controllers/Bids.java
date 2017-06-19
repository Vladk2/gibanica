

package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import models.*;
import play.*;


import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class Bids extends Controller {

    public static String loggedUser = session("connected");
    public static String tip = session("userType");
    public static String verified = session("verified");
    public static List<RequestFood> groceries = new ArrayList<>();
    public static List<Request> requests = new ArrayList<>();

    @SuppressWarnings("Duplicates")
    public static Result requests() {

        groceries.clear();
        requests.clear();
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani bidder.
        if(loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if(!tip.equals("bidder"))
            return redirect("/");
        // ----------------------------

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            stmt = connection.prepareStatement("Select name, amount, requestId from requestedFood;");
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                RequestFood rf = new RequestFood(result.getString(1), result.getInt(2),
                        result.getInt(3));
                groceries.add(rf);
            }
            stmt.close();

            stmt = connection.prepareStatement("Select requestId, fromDate, dueDate, restaurantId from requests where isActive = 1;");
            result = stmt.executeQuery();
            while (result.next()) {
                stmt2 = connection.prepareStatement("Select name from restaurants where restaurantId = ?;");
                stmt2.setString(1, result.getString(4));
                ResultSet result2 = stmt2.executeQuery();
                result2.next();
                String restName = result2.getString(1);
                Request rq = new Request(result.getInt(1), result.getString(2),
                        result.getString(3), restName);
                requests.add(rq);
            }
            stmt.close();
            stmt2.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return ok(bidRequests.render(requests, groceries));

    }
}





