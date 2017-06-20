

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.text.ParseException;


public class Bids extends Controller {

    public static String loggedUser = session("connected");
    public static String tip = session("userType");
    public static String userId = session("userId");
    public static String verified = session("verified");
    public static List<RequestFood> groceries = new ArrayList<>();
    public static List<Request> requests = new ArrayList<>();
    public static List<Offer> myOffersList = new ArrayList<>();

    @SuppressWarnings("Duplicates")
    public static Result requests() throws ParseException  {

        groceries.clear();
        requests.clear();
        myOffersList.clear();
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani bidder.
        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("bidder"))
            return redirect("/");
        // ----------------------------

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            PreparedStatement stmtDate = null;
            stmt = connection.prepareStatement("Select name, amount, requestId from requestedFood;");
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                RequestFood rf = new RequestFood(result.getString(1), result.getInt(2),
                        result.getInt(3));
                groceries.add(rf);
            }


            stmt = connection.prepareStatement("Select requestId, fromDate, dueDate, restaurantId from requests where isActive = 1;");
            result = stmt.executeQuery();
            while (result.next()) {

                stmt2 = connection.prepareStatement("Select name from restaurants where restaurantId = ?;");
                stmt2.setString(1, result.getString(4));
                ResultSet result2 = stmt2.executeQuery();
                result2.next();
                String restName = result2.getString(1);
                //------------------ date check
                Date today = Calendar.getInstance().getTime();
                Date datumIstekaRequesta=new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(3));
                Date datumPocetkaRequesta=new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(2));
                if (datumIstekaRequesta.compareTo(today) < 0 || datumPocetkaRequesta.compareTo(today) > 0 ) {
                    // nije vise aktivan request
                    System.out.println("\nDANAS JE " + today + "\nDEAKTIVIRA SE " + datumIstekaRequesta);
                    stmtDate = connection.prepareStatement("update requests set isActive=0 where requestId = ?;");
                    stmtDate.setInt(1, result.getInt(1));
                    stmtDate.executeUpdate();
                    stmtDate.close();
                }
                else {
                    //--------------
                    Request rq = new Request(result.getInt(1), result.getString(2),
                            result.getString(3), restName);
                    requests.add(rq);
                }

            }
            stmt = connection.prepareStatement("Select offerId, requestId, price, dueDate, message from offers where userId = ?;");

            stmt.setString(1, userId);
            result = stmt.executeQuery();
            while (result.next()) {
                Offer of = new Offer(result.getInt(1), result.getInt(2),result.getString(4),
                        result.getDouble(3), result.getString(5), userId);
                myOffersList.add(of);
            }



            stmt.close();
            stmt2.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return ok(bidRequests.render(requests, groceries, myOffersList));

    }

    public static Result offerAddedFlash() {
        flash("addOfferSuccess", "Your offer has been sent.");
        return redirect("/requests");

    }


    public static Result offerEditedFlash() {
        flash("addOfferSuccess", "Your offer has been edited successfully.");
        return redirect("/myOffers");

    }

    @SuppressWarnings("Duplicates")
    public static Result makeOffer() {


        JsonNode json = request().body().asJson();
        Offer offer = Json.fromJson(json, Offer.class);

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("insert into offers (requestId, price, dueDate, message, userId) values (?,?,?,?,?) ;");
            stmt.setInt(1, offer.reqId);
            stmt.setDouble(2, offer.price);
            stmt.setString(3, offer.dueDate);
            stmt.setString(4, offer.message);
            stmt.setString(5, userId);
            stmt.executeUpdate();


            stmt.close();

            return ok(Json.toJson(offer));

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");

    }

    @SuppressWarnings("Duplicates")
    public static Result myOffers() throws ParseException {

        groceries.clear();
        requests.clear();
        myOffersList.clear();
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani bidder.
        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("bidder"))
            return redirect("/");
        // ----------------------------

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            PreparedStatement stmtDate = null;
            stmt = connection.prepareStatement("Select name, amount, requestId from requestedFood;");
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                RequestFood rf = new RequestFood(result.getString(1), result.getInt(2),
                        result.getInt(3));
                groceries.add(rf);
            }


            stmt = connection.prepareStatement("Select requestId, fromDate, dueDate, restaurantId from requests where isActive = 1;");
            result = stmt.executeQuery();
            while (result.next()) {

                stmt2 = connection.prepareStatement("Select name from restaurants where restaurantId = ?;");
                stmt2.setString(1, result.getString(4));
                ResultSet result2 = stmt2.executeQuery();
                result2.next();
                String restName = result2.getString(1);
                //------------------ date check
                Date today = Calendar.getInstance().getTime();
                Date datumIstekaRequesta=new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(3));
                Date datumPocetkaRequesta=new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(2));
                if (datumIstekaRequesta.compareTo(today) < 0 || datumPocetkaRequesta.compareTo(today) > 0 ) {
                    // nije vise aktivan request
                    System.out.println("\nDANAS JE " + today + "\nDEAKTIVIRA SE " + datumIstekaRequesta);
                    stmtDate = connection.prepareStatement("update requests set isActive=0 where requestId = ?;");
                    stmtDate.setInt(1, result.getInt(1));
                    stmtDate.executeUpdate();
                    stmtDate.close();
                }
                else {
                    //--------------
                    Request rq = new Request(result.getInt(1), result.getString(2),
                            result.getString(3), restName);
                    requests.add(rq);
                }

            }
            stmt = connection.prepareStatement("Select offerId, requestId, price, dueDate, message from offers where userId = ?;");

            stmt.setString(1, userId);
            result = stmt.executeQuery();
            while (result.next()) {
                System.out.println("\nREQID su  " + result.getInt(2) + "\n");
                Offer of = new Offer(result.getInt(1), result.getInt(2),result.getString(4),
                        result.getDouble(3), result.getString(5), userId);
                myOffersList.add(of);
            }



            stmt.close();
            stmt2.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return ok(myOffers.render(requests, groceries, myOffersList));

    }

    @SuppressWarnings("Duplicates")
    public static Result editOffer() {


        JsonNode json = request().body().asJson();
        Offer offer = Json.fromJson(json, Offer.class);

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("update offers set dueDate = ?, price = ?, message = ? where requestId = ? and userId = ?;");
            stmt.setString(1, offer.dueDate);
            stmt.setDouble(2, offer.price);
            stmt.setString(3, offer.message);
            stmt.setInt(4, offer.reqId);
            stmt.setString(5, userId);
            stmt.executeUpdate();


            stmt.close();

            return ok(Json.toJson(offer));

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");

    }



}



