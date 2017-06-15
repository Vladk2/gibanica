package controllers;

import com.google.common.collect.ImmutableMap;
import models.User;
import play.*;
import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.db.Databases;
import play.db.Database;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import util.SendEmail;
import java.util.UUID;

/**
 * Created by vladk2 on 4/11/17.
 */

public class Register extends Controller {


    public static Result submit() throws SQLException {
        /* Uradjen primer transakcija sa Oracle sajta. Ima smisla ako se vise stvari odjednom insertuje u bazu */

        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();

        created.email = requestData.get("Regemail");
        created.password = requestData.get("pass");
        created.name = requestData.get("fName");
        created.surname = requestData.get("lName");
        created.verified = 0;
        created.tip = "guest";

        String verPass = requestData.get("repPass");

        if(verPass.equals(requestData.get("pass"))) {

            Connection connection = null;

            PreparedStatement regUser = null;

            PreparedStatement checkEmail = null;

            boolean mailExists = false;

            try {


                connection = DB.getConnection();

                connection.setAutoCommit(false);

                //provera ako je koriscen postojeci email
                checkEmail = connection.prepareStatement("select email from users where email=?");
                checkEmail.setString(1, created.email);
                ResultSet result = checkEmail.executeQuery();
                mailExists = result.next();



                if (!mailExists) {
                    regUser = connection.prepareStatement("Insert into users (name, surname, email, password, type, verified, verificationuuid) " +
                            "values (?, ?, ?, ?, ?, ?, ?);");
                    regUser.setString(1, created.name);
                    regUser.setString(2, created.surname);
                    regUser.setString(3, created.email);
                    regUser.setString(4, created.password);
                    regUser.setString(5, created.tip);
                    regUser.setString(6, Integer.toString(created.verified));
                    regUser.setString(7, UUID.randomUUID().toString());

                    regUser.executeUpdate();

                    connection.commit();

                    System.out.println("Success");

                    //testing
                    SendEmail.send("gogeccc@gmail.com", "foobar", "foobar");

                } else {
                    System.out.println("mail already exists");
                }



            } catch (SQLException sqle){
                sqle.printStackTrace();
                if (connection != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        connection.rollback();
                    } catch(SQLException excep) {
                        excep.printStackTrace();
                    }
                }
            } finally {
                connection.setAutoCommit(true);

                if (regUser != null) {
                    regUser.close();
                }

                if (checkEmail != null){
                    checkEmail.close();
                }

                if(connection != null){
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!mailExists) {
                session("connected", created.email);
                session("connectedFName", created.name);
                session("connectedLName", created.surname);
                session("connectedPass", created.password);
                session("userType", created.tip);
                session("verified", "0");
                return ok(submit.render(created));
            } else {
                return ok("Email address is already registered, please provide another email address");
            }

        }

        else return ok("Password does not match the confirm password");
    }

    public static Result verify(String id) throws SQLException {
        if(id.length() == 36) {

            Connection connection = null;
            PreparedStatement getUser = null;
            int resultSetSize = 0;
            PreparedStatement getCount = null;
            PreparedStatement updateVerified = null;

            User verified = new User();

            try {
                connection = DB.getConnection();
                getCount = connection.prepareStatement("select count(*) from baklava.users where verificationuuid=?;");
                getCount.setString(1, id);
                ResultSet count = getCount.executeQuery();
                count.next();
                resultSetSize = count.getInt(1);

                if (resultSetSize == 1) {
                    getUser = connection.prepareStatement("select * from baklava.users where verificationuuid=?;");
                    getUser.setString(1, id);
                    ResultSet resultSet = getUser.executeQuery();
                    resultSet.next();

                    verified.name = resultSet.getString(1);
                    verified.surname = resultSet.getString(2);
                    verified.email = resultSet.getString(3);
                    verified.password = resultSet.getString(4);
                    verified.tip = resultSet.getString(5);
                    verified.verified = 1;

                    connection.setAutoCommit(false);

                    updateVerified = connection.prepareStatement("update baklava.users set verified = 1 where verificationuuid = ?");
                    updateVerified.setString(1, id);
                    updateVerified.executeUpdate();


                    connection.commit();


                    session("connected", verified.email);
                    session("connectedFName", verified.name);
                    session("connectedLName", verified.surname);
                    session("connectedPass", verified.password);
                    session("userType", verified.tip);
                    session("verified", "1");
                    
                    return ok(views.html.verified.render(verified));
                } else {
                    return notFound();
                }

            } catch (SQLException sqle){

                try {
                    if (connection != null && !connection.getAutoCommit()) {
                        System.err.print("Transaction is being rolled back");
                        connection.rollback();
                    }
                } catch(SQLException excep) {
                    excep.printStackTrace();
                }
                sqle.printStackTrace();
            }
            finally {
                if (getUser != null) {
                    getUser.close();
                }
                if (getCount != null) {
                    getCount.close();
                }
                if(connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            }

        }

        //ako je dosao dovde, nesto nije ok
        return notFound();
    }

}
