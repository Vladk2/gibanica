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
import views.html.index;
import views.html.submit;

/**
 * Created by stefan on 4/11/17.
 */

public class Login extends Controller {

    public static Result login_form(){
        return ok(views.html.login_form.render());
    }

    public static Result submit(){
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.email = requestData.get("email");
        created.password = requestData.get("password");
        System.out.print(created.email);

        return ok(submit.render(created));
    }
}
