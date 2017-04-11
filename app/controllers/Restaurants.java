

package controllers;


        import play.*;


        import play.mvc.Controller;
        import play.mvc.Result;

        import views.html.restaurant;



public class Restaurants extends Controller {

    public static Result restaurants() {

        String loggedUser = session("connected");

        return ok(restaurant.render(loggedUser));
    }


}


