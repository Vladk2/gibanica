

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
    public static List<Request> acceptedRequests = new ArrayList<>();
    public static List<Offer> offersList = new ArrayList<>();

    @SuppressWarnings("Duplicates")
    public static Result requests() throws ParseException  {

        tip = session("userType");
        verified = session("verified");
        userId = session("userId");
        loggedUser = session("connected");

        setBids();

        return ok(bidRequests.render(requests, groceries, offersList));

    }

    @SuppressWarnings("Duplicates")
    public static Result myOffers() throws ParseException {

        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani bidder.
        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("bidder"))
            return redirect("/");
        // ----------------------------

        setBids();
        setAcceptedRequests();

        return ok(myOffers.render(requests, groceries, offersList, acceptedRequests));

    }

    @SuppressWarnings("Duplicates")
    public static Result offers() throws ParseException {


        tip = session("userType");
        verified = session("verified");
        userId = session("userId");
        loggedUser = session("connected");
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani rest-manager.

        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("rest-manager"))
            return redirect("/");
        // ----------------------------

        setBids();
        setAcceptedRequests();

        return ok(offers.render(requests, groceries, offersList, acceptedRequests));

    }




    public static Result offerAddedFlash() {
        flash("addOfferSuccess", "Your offer has been sent.");
        return redirect("/requests");

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

    public static Result acceptOffer() {

        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        Request req = Json.fromJson(json, Request.class);


        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("update requests set isActive = 0, acceptedOfferId = ? where requestId = ?;");
            stmt.setInt(1, req.acceptedOfferId);
            stmt.setInt(2, req.reqId);

            stmt.executeUpdate();


            stmt.close();

            return ok(Json.toJson(req));

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");
    }

    public static void setAcceptedRequests() {

        acceptedRequests.clear();
        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            stmt = connection.prepareStatement("select requestId, fromDate, dueDate, restaurantId, acceptedOfferId from requests where isActive = 0 and acceptedOfferId != 0;");
            ResultSet result = stmt.executeQuery();

            while (result.next()) {
                stmt2 = connection.prepareStatement("Select name from restaurants where restaurantId = ?;");
                stmt2.setString(1, result.getString(4));
                ResultSet result2 = stmt2.executeQuery();
                result2.next();
                String restName = result2.getString(1);
                Request rq = new Request(result.getInt(1), result.getString(2),
                        result.getString(3), restName, result.getInt(5));
                acceptedRequests.add(rq);
            }

            if(stmt != null)
                stmt.close();
            if(stmt2 != null)
                stmt2.close();


        } catch(SQLException sqle){
            sqle.printStackTrace();
        }
    }

    public static void setBids() throws ParseException  {

        groceries.clear();
        requests.clear();
        offersList.clear();


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


            stmtDate = connection.prepareStatement("Select requestId, fromDate from requests where isActive = 0 and acceptedOfferId = 0;");

            // ---- ako je datum pocetka requesta veci ili jednak danasnjem datumu, aktiviraj neaktivni request
            result = stmtDate.executeQuery();
            while (result.next()) {

                Date today = Calendar.getInstance().getTime();
                Date datumPocetkaRequesta=new SimpleDateFormat("yyyy-MM-dd").parse(result.getString(2));
                if (today.compareTo(datumPocetkaRequesta) > 0 ) {
                    stmtDate = connection.prepareStatement("update requests set isActive = 1  where requestId = ?;");
                    stmtDate.setInt(1, result.getInt(1));
                    stmtDate.executeUpdate();
                    stmtDate.close();
                }
                else {
                }

            }
            // ------------------------------
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

                    stmtDate = connection.prepareStatement("update requests set isActive = 0 where requestId = ?;");
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

            stmt = connection.prepareStatement("Select offerId, requestId, price, dueDate, message, userId from offers;");

            result = stmt.executeQuery();
            while (result.next()) {
                stmt2 = connection.prepareStatement("Select name, surname, email from users where userId = ?;");
                stmt2.setString(1, result.getString(6));
                ResultSet result2 = stmt2.executeQuery();
                result2.next();
                String mailBiddera = result2.getString(3);
                String imeBiddera = result2.getString(1) + " " + result2.getString(2);
                Offer of = new Offer(result.getInt(1), result.getInt(2),result.getString(4),
                        result.getDouble(3), result.getString(5), result.getString(6), imeBiddera, mailBiddera);
                offersList.add(of);
            }



            stmt.close();
            stmt2.close();

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }





}



