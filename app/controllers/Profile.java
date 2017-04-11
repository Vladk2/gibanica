package controllers;

import com.google.common.collect.ImmutableMap;
import models.User;
import play.*;
import play.data.DynamicForm;
import play.data.Form;
import play.db.Databases;
import play.db.Database;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.home;
import views.html.index;
import views.html.profile;
import views.html.submit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 4/11/17.
 */

public class Profile extends Controller {

    public static Result profile() {
        String loggedUser = session("connected");
        String loggedFName = session("connectedFName");
        String loggedLName = session("connectedLName");
        return ok(profile.render(loggedUser, loggedFName, loggedLName));
    }

    public static Result editProfile() {
        String loggedUser = session("connected");

        DynamicForm requestData = Form.form().bindFromRequest();
        String old_pw = requestData.get("old_password");
        String new_pw = requestData.get("new_password");

        Database database = Databases.createFrom(
                "baklava",
                "com.mysql.jdbc.Driver",
                "jdbc:mysql://localhost/baklava",
                ImmutableMap.of(
                        "user", "root",
                        "password", "gibanica"
                )
        );

        Connection connection = database.getConnection();

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

                if(connection.prepareStatement("Update users" +
                        " set password=" + "\"" + new_pw + "\"" +
                        " Where email=" + "\"" + loggedUser + "\"" + ";").execute()){
                    System.out.println("Password successfully changed !");
                }


                return ok("Password successfully changed !");
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

        return ok("This function must edit profile, not yet implemented");
    }

}


