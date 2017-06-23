package controllers;

import controllers.*;
import controllers.routes;
import org.junit.After;
import org.junit.Test;
import play.mvc.Result;
import org.junit.Before;
import play.test.FakeApplication;
import play.test.WithApplication;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;

/**
 * Created by stefan on 6/23/17.
 */
public class OrdersTest  extends WithApplication{
    private FakeApplication application;

    @Before
    public void startApp() {
      application = fakeApplication(inMemoryDatabase());
      start(application);
    }

    @After
    public void stopApp() {
      stop(application);
    }

    @Test
    public void orders() throws Exception {
        Result result = Orders.newOrder();
        assertEquals(OK, result.status());
    }

    @Test
    public void editOrder() throws Exception {
    }

    @Test
    public void newOrder() throws Exception {
    }

}