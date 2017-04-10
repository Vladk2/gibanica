package controllers;

import com.google.common.collect.ImmutableMap;
import play.*;
import play.db.Databases;
import play.db.Database;
import play.mvc.*;

import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class Application extends Controller {

    public static Result index() {
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
            ResultSet set = connection.prepareStatement("Select * from usertypes;").executeQuery();
            List rowValues = new ArrayList();
            while(set.next()){
                rowValues.add(set.getString(1));
            }
            //listaObjekata = (String []) rowValues.toArray(new String[rowValues.size()]);
            System.out.println(rowValues.get(0));
        } catch (SQLException e){
            e.printStackTrace();
        }
        return ok(index.render("Your new application is ready."));
    }

}
