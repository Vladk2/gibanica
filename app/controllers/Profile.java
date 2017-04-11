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
        return ok("This function must edit profile, not yet implemented");
    }

}


