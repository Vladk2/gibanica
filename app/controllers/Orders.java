package controllers;

import play.mvc.Controller;
import akka.actor.*;
import play.libs.F.*;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.orders;

import java.util.HashMap;

/**
 * Created by stefan on 6/17/17.
 */
public class Orders extends Controller {

    public static Result orders(){

        // iscitaj listu sa imenima hrane i pica za restoran u kome radi konobar ili
        // koji gost trenutno posecuje

        return ok(orders.render());
    }

    private static HashMap<String, ActorRef> clients = new HashMap<String, ActorRef>();

    public static WebSocket<String> proceed() {
        return WebSocket.withActor(OrdersPostman::props);
    }

    public static class OrdersPostman extends UntypedActor {

        public static Props props(ActorRef out) {
            return Props.create(OrdersPostman.class, out);
        }

        private final ActorRef out;

        public OrdersPostman(ActorRef out) {
            this.out = out;
        }

        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {
                clients.put(message.toString(), out);
                clients.get(message.toString()).tell("I received your message: " + message, self());
            }
        }
    }


}
