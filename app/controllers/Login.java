package controllers;

import com.google.common.collect.ImmutableMap;
import models.User;
import play.*;
import play.data.Form;
import play.db.Databases;
import play.db.Database;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import views.html.login_form;
import views.html.submit;

/**
 * Created by stefan on 4/11/17.
 */

public class Login extends Controller {

    final static Form<User> loginForm = Form.form(User.class);

    public static Result login_form(){
        return ok(login_form.render(loginForm));
    }

    public static Result submit(){
        Form<User> filledForm = Form.form(User.class).bindFromRequest(); // loginForm.bindFromRequest();
        User created = filledForm.get();
        System.out.print(created.email);

        return ok(submit.render(created));
    }
}
