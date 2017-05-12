import org.junit.*;
import play.db.DB;
import play.libs.F.*;
import play.mvc.*;
import play.test.*;

import java.sql.Connection;

import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.*;


/**
 * Simple (JUnit) tests that can call all parts of a play app.
 * If you are interested in mocking a whole application, see the wiki for more details.
 */
public class ApplicationTest {

    @Test
    public void checkRegistration() {
        Connection conn = DB.getConnection();

        assertThat(a).isEqualTo(2);
    }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Your new application is ready.");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Your new application is ready.");
    }


}
