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
import views.html.submit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 4/11/17.
 */

public class Login extends Controller {

    public static Result submit(){
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.email = requestData.get("email");
        created.password = requestData.get("password");


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
        try {
            /*ResultSet set = connection.prepareStatement("Select email, password from users where email="
                    + "\"" + created.email + "\"" + " and password=" + "\"" + created.password
                    + "\"" + ";").executeQuery();*/
            ResultSet set = connection.prepareStatement("Select password, email, name, surname, type from users where password="
                    + "\"" + created.password + "\" and email=" + "\"" + created.email + "\"" + ";").executeQuery();
            String email = "";
            String pw = "";
            String tip = "";
            String ime = "";
            String prezime = "";
            while(set.next()){
                pw = set.getString(1);
                email = set.getString(2);
                ime = set.getString(3);
                prezime = set.getString(4);
                tip = set.getString(5);
            }
            if(pw.equals(created.password) && email.equals(created.email)) {
                session("connected", email);
                session("connectedFName", ime);
                session("connectedLName", prezime);
                session("connectedPass", pw);
                session("userType", tip);
                String loggedUser = session("connected");
                return ok(home.render("Welcome",new play.twirl.api.Html("<center>Welcome, " + loggedUser + "!</center>") )); // login succedded
            }
            //listaObjekata = (String []) rowValues.toArray(new String[rowValues.size()]);
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                    System.out.println("Connection closed.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return ok("Wrong email or password.");
    }

    public static Result logout() {
        session().remove("connected");
        return ok(index.render("bla"));
    }
}


