package controllers;

import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by stefan on 4/11/17.
 */

public class Profile extends Controller {

    public static Result profile() {
        String loggedUser = session("connected");
        String loggedFName = session("connectedFName");
        String loggedLName = session("connectedLName");
        String loggedType = session("userType");
        String verified = session("verified");
        if(loggedUser == null || verified.equals("0"))
            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else
        return ok(profile.render(loggedUser, loggedFName, loggedLName, loggedType));
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


