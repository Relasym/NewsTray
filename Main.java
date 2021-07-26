import java.awt.*;

public class Main {

    public static void main(String[] args)  {

        boolean instanceSuccessful = false;
        int maxAttempts=5;
        int attempts=0;

        while(!instanceSuccessful && attempts<maxAttempts) {
            attempts++;

            try {
                NewsTray news = new NewsTray();
                instanceSuccessful=true;
            } catch (AWTException e) {
                System.out.println("Tray Error, attempt " + attempts + "/"+maxAttempts);
            }


        }

    }
}
