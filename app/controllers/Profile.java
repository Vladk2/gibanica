package controllers;

import models.RestSection;
import models.WorkTime;
import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 4/11/17.
 */

public class Profile extends Controller {

    @SuppressWarnings("Duplicates")
    public static Result profile() {
        String loggedUser = session("connected");
        String loggedFName = session("connectedFName");
        String loggedLName = session("connectedLName");
        String loggedType = session("userType");
        String verified = session("verified");

        if(loggedUser == null || verified.equals("0"))
            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else {
            /* liste za raspored stolova */
            List<RestSection> seats = new ArrayList<>();
            List<RestSection> sectors = new ArrayList<>();
            int rsize = 0;
            // -------------------
            /* lista za radno vreme */
            List<WorkTime> times = new ArrayList<>();
            // -------------------
            String posX;
            String posY;
            String sectorColor;

            double restSize=0;

            Connection connection = null;

            try {
                connection = DB.getConnection();

                ResultSet set = connection.prepareStatement("Select posX, posY, sectorColor from seatconfig where restaurantId = " +
                        "(select restaurantId from workers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();

                while(set.next()){
                    restSize++;
                    posX = set.getString(1);
                    posY = set.getString(2);
                    sectorColor = set.getString(3);
                    RestSection seat = new RestSection(sectorColor, posX, posY);
                    seats.add(seat);
                }

                ResultSet set2 = connection.prepareStatement("Select sectorName, sectorColor from sectornames where restaurantId = " +
                        "(select restaurantId from workers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();

                while (set2.next()) {
                    RestSection legend = new RestSection(set2.getString(1), set2.getString(2));
                    sectors.add(legend);
                }

                if (loggedType.equals("waiter") || loggedType.equals("bartender") || loggedType.equals("chef")) {
                    String statement =
                      String.format("select wd.date, wd.startTime, wd.endTime, wd.sectorName from workingDay as wd " +
                                    "left join users as u on wd.userId = u.userId where u.email = \"%s\";", loggedUser);

                    ResultSet working_schedule = connection.prepareStatement(statement).executeQuery();

                    while(working_schedule.next()){
                        WorkTime workTime =
                                new WorkTime(working_schedule.getString(1),
                                             working_schedule.getString(2),
                                             working_schedule.getString(3),
                                             working_schedule.getString(4));
                        System.out.println(workTime.date);
                        times.add(workTime);
                    }
                }

                restSize = Math.sqrt(restSize);
                rsize = (int)restSize;

                return ok(profile.render(loggedUser, loggedFName, loggedLName, loggedType, rsize, seats, sectors, times));

            } catch (SQLException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    try {
                        connection.close();
                    } catch (SQLException ez) {
                        ez.printStackTrace();
                    }
                }
            }

            return badRequest("Something strange happened ):");
        }
    }

    public static Result editProfile() {
        String loggedUser = session("connected");


        DynamicForm requestData = Form.form().bindFromRequest();
        String old_pw = requestData.get("old_password");
        String new_pw = requestData.get("new_password");
        String new_fname = requestData.get("fname");
        String new_lname = requestData.get("lname");

        Connection connection = DB.getConnection();

        try{
            ResultSet set = connection.prepareStatement("Select password, email from users where password="
                        + "\"" + old_pw + "\" and email=" + "\"" + loggedUser + "\"" + ";").executeQuery();

            String email = "";
            String pw = "";

            while(set.next()){
                pw = set.getString(1);
                email = set.getString(2);
            }

            if(pw.equals(old_pw) && email.equals(loggedUser)) {

                if(connection.prepareStatement("Update users" + " set password=" +
                        "\"" + new_pw + "\" ,name=" + "\"" + new_fname + "\" , surname=" +
                        "\"" + new_lname + "\"" + " Where email=" + "\"" + loggedUser +
                        "\"" + ";").execute()){

                }

                session("connectedFName", new_fname);
                session("connectedLName", new_lname);

                return ok(home.render("Welcome", new play.twirl.api.Html("<center>Good job, <b>" +
                        loggedUser + "</b>, your account info has been changed!</center>")));

            }
            else if((old_pw.equals("")) && (new_pw.equals(""))) {
                if (connection.prepareStatement("Update users" +
                        " set name=" + "\"" + new_fname + "\" , surname=" + "\"" + new_lname + "\"" +
                        " Where email=" + "\"" + loggedUser + "\"" + ";").execute()) {

                }


                session("connectedFName", new_fname);
                session("connectedLName", new_lname);
                return ok(home.render("Welcome", new play.twirl.api.Html("<center>Good job, <b>" + loggedUser + "</b>, your account info has been changed!</center>")));
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
           if(connection != null){
                try {
                    connection.close();
                } catch (SQLException q){
                    q.printStackTrace();
                }
            }
        }

        return ok("Wrong password");
    }

}


