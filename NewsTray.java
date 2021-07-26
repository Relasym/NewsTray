import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsTray {
    private String website = "https://orf.at/";
    private String[] rssFeeds = {"https://rss.orf.at/news.xml",
            "https://rss.orf.at/sport.xml",
            "https://rss.orf.at/debatten.xml",
            "https://rss.orf.at/help.xml",
            "https://rss.orf.at/science.xml",
            "https://rss.orf.at/oe3.xml",
            "https://rss.orf.at/fm4.xml",
            "https://rss.orf.at/oesterreich.xml",
            "https://rss.orf.at/ooe.xml"};
    //    private String currentFilePath;  //dirty hack do not steal
    private boolean keepRunning = true;
    private boolean pauseScanning = false;
    private final SystemTray tray;
    private final TrayIcon trayIcon;
    private final PopupMenu popupMenu;
    private final HashSet<String> usedHeadlines = new HashSet<>();


    public NewsTray() throws AWTException {
        //setup system tray
        tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage("IconSmall.jpg");
        trayIcon = new TrayIcon(image, "ORF");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("News Tray");
        tray.add(trayIcon);

        //setup popup menu
        popupMenu = new PopupMenu();
        MenuItem exitItem = new MenuItem("Exit");
        CheckboxMenuItem pauseItem = new CheckboxMenuItem("Pause Scanning");
        MenuItem infoItem = new MenuItem("NewsTray");
        popupMenu.add(infoItem);
        infoItem.setEnabled(false);
        popupMenu.addSeparator();
        popupMenu.add(pauseItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);
        trayIcon.setPopupMenu(popupMenu);

        //close on clicking exit button
        ActionListener closeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopRunning();
            }
        };
        exitItem.addActionListener(closeListener);

        //pause on clicking pause button
        ItemListener pauseListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                pauseScanning = !pauseScanning;
            }
        };
        pauseItem.addItemListener(pauseListener);


        //run main loop
        mainLoop();
    }

    public void mainLoop() {
        long updateTime = System.nanoTime();
        long delayTime = 60 * (long) Math.pow(10, 9);
        ArrayList<String> headlines = new ArrayList<>();

        try {
            headlines.addAll(getHeadlinesFromWebsite(website));
        } catch (FileNotFoundException e) {
            System.out.println("Could not get headlines");
            System.out.println(e.toString());
        }

        try {
            headlines.addAll(getHeadlinesFromRSSFeed(rssFeeds[0]));
        } catch (IOException e) {
            System.out.println("Could not get headlines");
            System.out.println(e.toString());
        }


        if (headlines.size() == 0) {
            keepRunning = false;
        } else {
            usedHeadlines.addAll(headlines);
        }


        while (keepRunning) {
            if (System.nanoTime() > updateTime + delayTime) {
                if (!pauseScanning) {
                    headlines.clear();


                    //get headlines from website
                    try {
                        headlines = getHeadlinesFromWebsite(website);
                    } catch (FileNotFoundException e) {
                        System.out.println("Could not get headlines");
                        System.out.println(e.toString());
                    }

                    //show new headlines from website
                    for (String currentHeadline : headlines) {
                        if (!usedHeadlines.contains(currentHeadline)) {
                            showMessage(currentHeadline);
                            System.out.println("found new Headline");
                            usedHeadlines.add(currentHeadline);
                        }
                    }

                    headlines.clear();
                    //get headlines from RSS feed
                    try {
                        headlines = getHeadlinesFromRSSFeed(rssFeeds[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //show new headlines from rss
                    for (String currentHeadline : headlines) {
                        if (!usedHeadlines.contains(currentHeadline)) {
                            showMessage(currentHeadline);
                            System.out.println("found new Headline");
                            usedHeadlines.add(currentHeadline);
                        }
                    }

                    System.out.println("Getting headlines");
                }
                updateTime = updateTime + delayTime;
            }


            //wait some time
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("Thread sleep interrupted. We don't care.");
            }

        }

        tray.remove(trayIcon);
    }


    public void startRunning() {
        keepRunning = true;
    }

    public void stopRunning() {
        keepRunning = false;
    }


    public ArrayList<String> getHeadlinesFromWebsite(String targetURL) throws FileNotFoundException {
        String filePath = "";
        try {
            filePath = getSite(targetURL);
        } catch (IOException e) {
            throw new FileNotFoundException("Could not get Website");
        }

        ArrayList<String> headlines = new ArrayList<>();
        if (!filePath.equals("")) {
            try {
                headlines = getHeadlinesFromHTML(filePath);
            } catch (IOException e) {
                throw new FileNotFoundException("No headlines available");
            }
        }
        return headlines;

    }

    public String getSite(String targetURL) throws IOException {
        URL url = new URL(targetURL);
        String filePath = "pages//";
        String fileName = "page";
        String fileEnd = ".html";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date date = new Date();
        fileName = format.format(date);

        String currentFilePath = filePath + fileName + fileEnd;
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(currentFilePath));

        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }
        return currentFilePath;
    }

    public ArrayList<String> getHeadlinesFromHTML(String filePath) throws IOException {
        FileReader file = new FileReader(filePath);
        BufferedReader reader = new BufferedReader(file);
        String line;
        line = reader.readLine();
        ArrayList<String> headlines = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?<=aria-expanded=\"false\">            )(.*?)(?=         </a>      </h3>)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            headlines.add(matcher.group());
        }
        return headlines;
    }

    public ArrayList<String> getHeadlinesFromRSSFeed(String targetURL) throws IOException {
        String filePath;
        filePath = getXMLFromRSSFeed(targetURL);
        ArrayList<String> headlines = new ArrayList<>();
        headlines = getHeadlinesFromXML(filePath);
        return headlines;
    }

    public String getXMLFromRSSFeed(String targetURL) throws IOException {
        URL url = new URL(targetURL);
        String filePath = "pages//";
        String fileName = "RSS"; //no used
        String fileEnd = ".xml";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date date = new Date();
        fileName = "RSS" + format.format(date);
        String currentFilePath = filePath + fileName + fileEnd;

        InputStream xmlSource = new URL(targetURL).openStream();
        Files.copy(xmlSource, Paths.get(currentFilePath), StandardCopyOption.REPLACE_EXISTING);

        return currentFilePath;
    }

    public ArrayList<String> getHeadlinesFromXML(String filePath) throws IOException {
        ArrayList<String> headlines = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));
            document.getDocumentElement().normalize();
            NodeList itemList = document.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                headlines.add(itemList.item(i).getChildNodes().item(1).getTextContent()); //item 0 is the text between the tags!
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            System.out.println("Failed To Parse XML");
        }
        return headlines;
    }

    public void showMessage(String message) {
        trayIcon.displayMessage("ORF NEWS", message, TrayIcon.MessageType.NONE);
    }
}