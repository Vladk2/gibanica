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
import java.util.HashMap;
import java.util.List;

/**
 * Created by Goran
 */

public class Friends extends Controller {

    public enum list {SEARCH, FRIENDS, PENDING}

    public static Result friendsPage() {
        return ok(friends.render());
    }

    public static Result friendsSearchApi(){
        // test
        session("connected", "gogeccc@gmail.com");
        session("userId", "17");

        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        JsonNode jsonReq = request().body().asJson();
        FriendsSearchData friendsSearchData = Json.fromJson(jsonReq, FriendsSearchData.class);
        ArrayList<FriendsSearchData> friendsSearchDataList = null;

        try {
            friendsSearchDataList = Friends.getFriends(friendsSearchData.name, friendsSearchData.surname, list.SEARCH);
        }
        catch (SQLException e) {
            System.out.println("nesto otislo do vraga u friendsSearchApi");
            e.printStackTrace();
        }

        return ok(Json.toJson(friendsSearchDataList));

    }

    public static Result friendsGetAllApi() {
        // test
        session("connected", "gogeccc@gmail.com");
        session("userId", "17");

        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }
        ArrayList<FriendsSearchData> friendsSearchDataList = null;

        try {
            friendsSearchDataList = Friends.getFriends("", "", list.FRIENDS);
        }
        catch (SQLException e) {
            System.out.println("nesto otislo do vraga u friendsGetAllApi");
            e.printStackTrace();
        }

        return ok(Json.toJson(friendsSearchDataList));

    }

    public static Result friendsGetPendingApi() {
        // test
        session("connected", "gogeccc@gmail.com");
        session("userId", "17");

        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }
        ArrayList<FriendsSearchData> friendsSearchDataList = null;

        try {
            friendsSearchDataList = Friends.getFriends("", "", list.PENDING);
        }
        catch (SQLException e) {
            System.out.println("nesto otislo do vraga u friendsGetPendingApi");
            e.printStackTrace();
        }

        return ok(Json.toJson(friendsSearchDataList));

    }
    public static ArrayList<FriendsSearchData> getFriends(String name, String surname, Friends.list which) throws SQLException{
        //imam userId u sesiji, gud
        //mogao bih ovo preko negog joina, ali NEEEE
        //prvo nabavi ljude koji se poklapaju sa name i surname, ez
        // U MAPU USERID: USERSEARCHDATA - da ne bih morao da prolazim kroz celu listu za svaki friendship

        // ukloni sve specijalne karaktere (razmaci su ok, ne uklanjamo ih)
        name = name.replaceAll("[^A-Za-z0-9 ]", "");
        surname = surname.replaceAll("[^A-Za-z0-9 ]", "");

        PreparedStatement friendsSearchQuery1 = null;
        PreparedStatement friendsSearchQuery2 = null;
        ResultSet friendsSearchResultSet1 = null;
        ResultSet friendsSearchResultSet2 = null;
        Connection connection = null;

        ArrayList<FriendsSearchData> friendsSearchDataList = new ArrayList<FriendsSearchData>();
        HashMap<Integer, FriendsSearchData> friendsSearchDataMap = new HashMap<Integer, FriendsSearchData>();

        try {

            connection = DB.getConnection();
            String userIdString = session("userId");
            int userId = Integer.parseInt(userIdString == null ? "0" : userIdString);

            if (which == list.FRIENDS || which == list.PENDING) {
                // self sufficient querry
                String acceptedString = which == list.FRIENDS ? "1" : "0";
                boolean acceptedBoolean = which == list.FRIENDS;

                friendsSearchQuery1 = connection.prepareStatement("(select name, surname, userId, 0 " + //0 za sent
                        "from baklava.users " +
                        "where userId in (select friendId1 " +
                        "                 from baklava.friendships " +
                        "                 where friendId2 = ? and accepted = " + acceptedString + " and deleted = 0)) " +
                        "union " +
                        "(select name, surname, userId, 1 " + //1 za sent
                        "from baklava.users " +
                        "where userId in (select friendId2 " +
                        "                 from baklava.friendships " +
                        "                 where friendId1 = ? and accepted = " + acceptedString + " and deleted = 0)) " +
                        ";");
                friendsSearchQuery1.setInt(1, userId);
                friendsSearchQuery1.setInt(2, userId);
                friendsSearchResultSet1 = friendsSearchQuery1.executeQuery();

                while (friendsSearchResultSet1.next()) {
                    friendsSearchDataList.add(
                            new FriendsSearchData(
                                    friendsSearchResultSet1.getString(1),
                                    friendsSearchResultSet1.getString(2),
                                    friendsSearchResultSet1.getInt(3),
                                    friendsSearchResultSet1.getBoolean(4), //sent
                                    !friendsSearchResultSet1.getBoolean(4), //received = !sent
                                    acceptedBoolean
                            )
                    );
                }
            }
            else if (which == list.SEARCH) {
                //prvo nabavi rezultate pretrage
                friendsSearchQuery1 = connection.prepareStatement(
                        "select name, surname, userId from baklava.users " +
                                "where verified=1 and type='guest' " +
                                "and name like ? and surname like ? " +
                                "and userId not like ?"); //ne treba mi moj profil u listi rezultata pretrage
                //neophodno za dodati %% zbog like operatora u bazi
                friendsSearchQuery1.setString(1, "%" + name + "%");
                friendsSearchQuery1.setString(2, "%" + surname + "%");
                friendsSearchQuery1.setInt(3, userId);
                friendsSearchResultSet1 = friendsSearchQuery1.executeQuery();

                //spakuj rezultate pretrage u mapu
                while (friendsSearchResultSet1.next()) {
                    friendsSearchDataMap.put(
                            friendsSearchResultSet1.getInt(3),
                            new FriendsSearchData(
                                    friendsSearchResultSet1.getString(1),
                                    friendsSearchResultSet1.getString(2),
                                    friendsSearchResultSet1.getInt(3)
                            )
                    );
                }

                //sad nabavi sve frienshipe u kojima friendId1 = userid ili friendid2 = userid
                friendsSearchQuery2 = connection.prepareStatement("select friendId1, friendId2, accepted from baklava.friendships " +
                        "where (friendId1 = ? or friendId2 = ?) and deleted = 0");
                friendsSearchQuery2.setInt(1, userId);
                friendsSearchQuery2.setInt(2, userId);
                friendsSearchResultSet2 = friendsSearchQuery2.executeQuery();

                //prolazim kroz result set 2, biram odgovarajuci kljuc i podesavam odgovarajuce vrednosti za sent i accepted
                while (friendsSearchResultSet2.next()) {
                    int friendId1 = friendsSearchResultSet2.getInt(1);
                    int friendId2 = friendsSearchResultSet2.getInt(2);
                    boolean accepted = friendsSearchResultSet2.getBoolean(3);
                    boolean sent = friendId1 == userId;
                    boolean received = friendId2 == userId;
                    // ocekujem konziztentnost; ili je poslato ili primljeno, ne oba
                    int friendId = sent && !received ? friendId2 : friendId1;

                    //podesi odgovarajuca polja u rezultatu pretrage
                    if (friendsSearchDataMap.get(friendId) != null) { //ima li ga u mapi rezultata
                        friendsSearchDataMap.get(friendId).sent = sent;
                        friendsSearchDataMap.get(friendId).received = received;
                        friendsSearchDataMap.get(friendId).accepted = accepted;
                    }
                    else { //ima ga u prijateljima ali ga nema u rezultatima pretrage; skip
                    }

                    //ne zanimaju me oni koji nisu u friendships
                    // oni ce imati accepted podeseno na false po defaultu
                }

                // u listu radi serijalizacije
                friendsSearchDataList = new ArrayList<FriendsSearchData>(friendsSearchDataMap.values());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (friendsSearchQuery1 != null) {
                friendsSearchQuery1.close();
            }
            if (friendsSearchQuery2 != null) {
                friendsSearchQuery2.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return friendsSearchDataList;
    }
}


