

package controllers;


        import com.google.common.collect.ImmutableMap;
        import models.Restaurant;
        import play.*;


        import play.data.DynamicForm;
        import play.data.Form;
        import play.db.Database;
        import play.db.Databases;
        import play.mvc.Controller;
        import play.mvc.Result;

        import views.html.home;
        import views.html.restaurant;

        import java.sql.Connection;
        import java.sql.SQLException;


public class Restaurants extends Controller {

    public static Result restaurants() {


        String loggedUser = session("connected");

        return ok(restaurant.render(loggedUser));
    }


    public static Result addRestaurant() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Restaurant created = new Restaurant();
        created.name = requestData.get("rname");
        created.description = requestData.get("rdesc");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije.
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into restaurants (name, description) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.description + "\")" + ";").execute()) {
                System.out.println("Success");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Restaurant has been added successfully!</center>") ));

    }

    public static Result addRestaurantManager() {
        DynamicForm requestData = Form.form().bindFromRequest();
        RestaurantManager created = new RestaurantManager();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije.
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 1"
                    + ", \"rest-manager\")" + ";").execute()) {
                System.out.println("Success");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Restaurant manager has been added successfully!</center>") ));


    }

}


