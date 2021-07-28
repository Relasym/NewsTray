import java.awt.*;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        boolean instanceSuccessful = false;
        int maxAttempts = 3;
        int attempts = 0;

        while (!instanceSuccessful && attempts < maxAttempts) {
            attempts++;
            try {
                new NewsTray();
                instanceSuccessful = true;
            } catch (AWTException e) {
                System.out.println("Tray Error, attempt " + attempts + "/" + maxAttempts);
            } catch (IOException e) {
                System.out.println("Connection Issue");
            }
        }

    }
}
