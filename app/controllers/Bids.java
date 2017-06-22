

package controllers;


import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import models.*;
import net.sf.ehcache.search.expression.Not;
import play.*;

import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;


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

        tip = session("userType");
        verified = session("verified");
        userId = session("userId");
        loggedUser = session("connected");


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
        userId = session("userId");
        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("insert into offers (requestId, price, dueDate, message, userId, status) values (?,?,?,?,?,?) ;");
            stmt.setInt(1, offer.reqId);
            stmt.setDouble(2, offer.price);
            stmt.setString(3, offer.dueDate);
            stmt.setString(4, offer.message);
            stmt.setString(5, userId);
            stmt.setString(6, "pending");
            stmt.executeUpdate();


            stmt.close();

            return ok(Json.toJson(offer));

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");

    }

    public static Result bidderNotifications() {

        tip = session("userType");
        verified = session("verified");
        userId = session("userId");
        loggedUser = session("connected");
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani bidder.
        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("bidder"))
            return redirect("/");
        // ----------------------------

        List<Notification> notifikacije = new ArrayList<>();

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            PreparedStatement stmtDate = null;
            stmt = connection.prepareStatement("Select message, notificationId from notifications where userId = ?;");
            stmt.setInt(1, Integer.parseInt(userId));
            ResultSet result = stmt.executeQuery();

            while (result.next()) {
                Notification nf = new Notification(result.getInt(2),Integer.parseInt(userId),result.getString(1), 0);
                notifikacije.add(nf);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

            return ok(bidderHistory.render(notifikacije));
    }


    @SuppressWarnings("Duplicates")
    public static Result editOffer() {


        JsonNode json = request().body().asJson();
        Offer offer = Json.fromJson(json, Offer.class);
        userId = session("userId");

        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("update offers set dueDate = ?, price = ?, message = ? where requestId = ? and userId = ?;");
            stmt.setString(1, offer.dueDate);
            stmt.setDouble(2, offer.price);
            stmt.setString(3, offer.message);
            stmt.setInt(4, offer.reqId);
            stmt.setString(5, userId);
            stmt.executeUpdate();

            int offerId = offer.offerId;

            for(ActorRef klijent : connectedManagers.values()){
                klijent.tell(Integer.toString(offerId),  ActorRef.noSender());
            }

            stmt.close();

            return ok(Json.toJson(offer));

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");

    }

    public static Result acceptOffer() throws JsonProcessingException {

        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        Request req = Json.fromJson(json, Request.class);


        Connection connection = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;

        try {

            connection = DB.getConnection();

            connection.setAutoCommit(false);

            stmt = connection.prepareStatement("update requests set isActive = 0, acceptedOfferId = ? where requestId = ?;");
            stmt.setInt(1, req.acceptedOfferId);
            stmt.setInt(2, req.reqId);
            stmt.executeUpdate();

            stmt = connection.prepareStatement("update offers set status = ? where offerId = ?;");
            stmt.setString(1, "accepted");
            stmt.setInt(2, req.acceptedOfferId);
            stmt.executeUpdate();

            stmt = connection.prepareStatement("update offers set status = ? where offerId != ? and requestId = ?;");
            stmt.setString(1, "rejected");
            stmt.setInt(2, req.acceptedOfferId);
            stmt.setInt(3, req.reqId);
            stmt.executeUpdate();


            stmt = connection.prepareStatement("select userId from offers where offerId = ? ;");
            stmt.setInt(1, req.acceptedOfferId);
            stmt.executeQuery();
            ResultSet result = stmt.executeQuery();

            int acceptedUserId = 0;

                if (result.next()) {

                        acceptedUserId = result.getInt(1);
                        String acceptedMessage = "Congratulations, " + req.restName + " accepted your offer for request #" + req.reqId + "!";
                        stmt = connection.prepareStatement("insert into notifications (userId, message, seen) values (?,?,0) ;");
                        stmt.setInt(1, acceptedUserId);
                        stmt.setString(2, acceptedMessage);
                        stmt.executeUpdate();

                        stmt = connection.prepareStatement("select last_insert_id();");
                        result = stmt.executeQuery();
                        result.next();
                        int notifyId = result.getInt(1);
                        Notification notif = new Notification(notifyId, acceptedUserId, acceptedMessage, req.reqId);

                        ObjectMapper mapper = new ObjectMapper();
                        String notifString = mapper.writeValueAsString(notif);
                        if(clients.get(Integer.toString(acceptedUserId)) != null) {
                            clients.get(Integer.toString(acceptedUserId)).tell(notifString, ActorRef.noSender());
                        }

                }

                stmt = connection.prepareStatement("select userId from offers where offerId != ? and requestId = ? ;");
                stmt.setInt(1, req.acceptedOfferId);
                stmt.setInt(2, req.reqId);
                stmt.executeQuery();
                result = stmt.executeQuery();
                int rejectedUserId = 0;
                while (result.next()) {

                    rejectedUserId = result.getInt(1);
                    String rejecteddMessage = "Sorry, " + req.restName + " refused your offer for request #" + req.reqId + "!";
                    stmt = connection.prepareStatement("insert into notifications (userId, message, seen) values (?,?,0) ;");
                    stmt.setInt(1, rejectedUserId);
                    stmt.setString(2, rejecteddMessage);

                    stmt2 = connection.prepareStatement("select last_insert_id();");
                    result = stmt2.executeQuery();
                    result.next();
                    int notifyId = result.getInt(1);
                    Notification notif = new Notification(notifyId, rejectedUserId, rejecteddMessage, req.reqId);

                    ObjectMapper mapper = new ObjectMapper();
                    String notifString = mapper.writeValueAsString(notif);
                    if( clients.get(Integer.toString(rejectedUserId)) != null) {
                        clients.get(Integer.toString(rejectedUserId)).tell(notifString, ActorRef.noSender());
                    }
                    stmt.executeUpdate();

            }
                Notification notification = new Notification(0,0,null,req.reqId,"hideReq");
                ObjectMapper mapper = new ObjectMapper();
                String notifString = mapper.writeValueAsString(notification);
            for(ActorRef klijent : clients.values()){

                klijent.tell(notifString,  ActorRef.noSender());
            }

            connection.commit();
            stmt.close();

            return ok(Json.toJson(req));

        } catch (SQLException sql){
            sql.printStackTrace();
            if(connection != null){
                try {
                    connection.rollback();
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return internalServerError("Something strange happened");
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

    public static Result seenNotifications() throws SQLException {

        JsonNode j =  request().body().asJson();
        Notification n = Json.fromJson(j, Notification.class);

        Connection connection = null;
        PreparedStatement stmt = null;

        String query = "update notifications set seen = 1 where userId = ?;";

        try {

            connection = DB.getConnection();
            connection.setAutoCommit(false);
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, n.userId);
            stmt.executeUpdate();

            connection.commit();

            return ok();
        }
        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back\n");
                    connection.rollback();

                }
                if (stmt != null) {
                    stmt.close();
                }

            } catch(SQLException excep) {
                excep.printStackTrace();
            }
            sqle.printStackTrace();
        }
        finally {

            if(connection != null) {
                connection.close();
            }

        }
        return internalServerError("Something strange happened");

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


    private static ConcurrentHashMap<String, ActorRef> clients = new ConcurrentHashMap<String, ActorRef>();
    private static ConcurrentHashMap<String, ActorRef> connectedManagers = new ConcurrentHashMap<>();

    public static WebSocket<String> notifyManager() {
        return WebSocket.withActor(Bids.OffersPostman::props);
    }

    public static class OffersPostman extends UntypedActor {

        public static Props props(ActorRef out) {
            return Props.create(Bids.OffersPostman.class, out);
        }

        private final ActorRef out;
        private String user;

        public OffersPostman(ActorRef out) {
            this.out = out;
        }

        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                HashMap<String, String> request_message = objectMapper.readValue(message.toString(), HashMap.class);
                List<Notification> userNotifications = new ArrayList<>();
                if(request_message.get("type").equals("restmanConnection")){
                    connectedManagers.put(request_message.get("userId"), out);
                    this.user = request_message.get("userMail");
                    System.out.print("konektovao se " + request_message.get("userId"));
                }
                if(request_message.get("type").equals("bidderConnection")) {
                    clients.put(request_message.get("userId"), out);
                    this.user = request_message.get("userMail");

                    try (Connection connection = DB.getConnection()) {

                        PreparedStatement stmt = null;

                        stmt = connection.prepareStatement("select userId from users where email = ?;");
                        stmt.setString(1, user);
                        stmt.executeQuery();
                        ResultSet result = stmt.executeQuery();
                        int realUserId = 0;

                        if(result.next()) {
                            realUserId = result.getInt(1);
                        }
                        stmt = connection.prepareStatement("select notificationId, message from notifications where userId = ? and seen = 0;");
                        stmt.setInt(1,realUserId);
                        stmt.executeQuery();
                        result = stmt.executeQuery();
                        int notifyNo = 0;
                        int notifyId = 0;
                        String notifyMessage = "";

                        while(result.next()){

                            notifyId = result.getInt(1);
                            notifyMessage = result.getString(2);
                            Notification notif = new Notification(notifyId, realUserId, notifyMessage, 0);
                            userNotifications.add(notif);
                            notifyNo++;

                        }
                        stmt.close();


                    } catch (SQLException sqle) {
                        sqle.printStackTrace();
                    }


                    for(ActorRef klijent : clients.values())
                    {
                     for(Notification notifikacija : userNotifications) {
                         ObjectMapper mapper = new ObjectMapper();
                         String notifString = mapper.writeValueAsString(notifikacija);
                         klijent.tell(notifString, self());
                     }
                    }
                }
            }
        }

        public void postStop() throws Exception {
            System.out.println("\nWebsocket closing for: " + this.user);
            clients.remove(this.user);
        }
    }





}



