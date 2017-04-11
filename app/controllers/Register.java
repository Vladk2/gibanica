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
 * Created by vladk2 on 4/11/17.
 */

public class Register extends Controller {


    public static Result submit(){
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.email = requestData.get("Regemail");
        created.password = requestData.get("pass");
        created.name = requestData.get("fName");
        created.surname = requestData.get("lName");
        String verPass = requestData.get("repPass");
        if(verPass.equals(requestData.get("pass"))) {
            flash("success", "The item has been created");
            return ok(submit.render(created));

        }
        else return ok(index.render("olalaaa"));

    }
}
