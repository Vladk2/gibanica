package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.FriendsSearchData;
import models.User;
import org.h2.command.Prepared;
import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Goran
 */

public class Friends extends Controller {

    public static Result friendsPage() {
        return ok(friends.render());
    }

    public static Result friendsSearchApi() throws SQLException{

        String loggedIn = session("connected");

        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        JsonNode jsonReq = request().body().asJson();
        FriendsSearchData friendsSearchData = Json.fromJson(jsonReq, FriendsSearchData.class);
        // ukloni sve specijalne karaktere
        friendsSearchData.name = friendsSearchData.name.replaceAll("[^A-Za-z0-9 ]", "");
        friendsSearchData.surname = friendsSearchData.surname.replaceAll("[^A-Za-z0-9 ]", "");

        PreparedStatement friendsSearchQuery = null;
        ResultSet friendsSearchResultSet = null;
        Connection connection = null;

        ArrayList<FriendsSearchData> friendsSearchDataList = new ArrayList<FriendsSearchData>();

        try {
            connection = DB.getConnection();
            friendsSearchQuery = connection.prepareStatement(
                    "select name, surname, userId from baklava.users " +
                            "where verified=1 and type='guest' and name like ? and surname like ?");
            friendsSearchQuery.setString(1, "%" + friendsSearchData.name + "%");
            friendsSearchQuery.setString(2, "%" + friendsSearchData.surname + "%");
            friendsSearchResultSet = friendsSearchQuery.executeQuery();

            while (friendsSearchResultSet.next()) {
                friendsSearchDataList.add(
                        new FriendsSearchData(
                                friendsSearchResultSet.getString(1),
                                friendsSearchResultSet.getString(2),
                                friendsSearchResultSet.getInt(3)
                        )
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (friendsSearchQuery != null) {
                friendsSearchQuery.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return ok(Json.toJson(friendsSearchDataList));

    }

}


