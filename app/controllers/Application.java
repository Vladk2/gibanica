package controllers;

import play.db.DB;

import views.html.*;

import play.mvc.Controller;
import play.mvc.Result;
import play.Routes; // freaking s***!
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import static views.html.home.render;

public class Application extends Controller {


    public static Result jsRoutes() {
        response().setContentType("text/javascript");
        return ok(Routes.javascriptRouter("myJsRoutes",
                routes.javascript.Restaurants.addVictualAJAX(),
                routes.javascript.Restaurants.addRestInfoAJAX(),
                routes.javascript.Restaurants.addDrinkAJAX(),
                routes.javascript.Restaurants.saveSeatConf(),
                routes.javascript.Restaurants.addSeatSection(),
                routes.javascript.Restaurants.editRestaurant(),
                routes.javascript.Restaurants.editVictualAJAX(),
                routes.javascript.Restaurants.editDrinkAJAX(),
                routes.javascript.Restaurants.editSeatConf(),
                routes.javascript.Restaurants.editSeatSection(),
                routes.javascript.Restaurants.removeVictualOrDrink(),
                routes.javascript.Restaurants.addWorkTime(),
                routes.javascript.Restaurants.removeSector()

                )
        );
    }


    public static Result index() {
        String loggedUser = session("connected");
        String verified = session("verified");
        String tip = session("userType");
        String name = session("connectedFName");
        if(loggedUser==null) {

            Connection connection = DB.getConnection();
            try {
                ResultSet set = connection.prepareStatement("Select * from usertypes;").executeQuery();
                List rowValues = new ArrayList();
                while (set.next()) {
                    System.out.println(set.getString(1));
                    rowValues.add(set.getString(1));
                }
                //listaObjekata = (String []) rowValues.toArray(new String[rowValues.size()]);
                //   System.out.println(rowValues.get(0));


            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return ok(index.render("Your new application is ready."));
        }
        else if((tip.equals("bidder") || tip.equals("waiter") || tip.equals("chef") || tip.equals("bartender")) && verified.equals("0"))
            return ok(firstLog.render(loggedUser));
        else if((tip.equals("guest") && verified.equals("0")))
            return ok(firstLogGuest.render(name, loggedUser));
        else return ok(render("Welcome",new play.twirl.api.Html("<center><h2>Welcome, " + loggedUser + "!</h2></center>") ));
    }




}
