import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
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
    private final String website = "https://orf.at/";
    private final String[] rssFeeds = {"https://rss.orf.at/news.xml",  //currently just using the first
            "https://rss.orf.at/sport.xml",
            "https://rss.orf.at/debatten.xml",
            "https://rss.orf.at/help.xml",
            "https://rss.orf.at/science.xml",
            "https://rss.orf.at/oe3.xml",
            "https://rss.orf.at/fm4.xml",
            "https://rss.orf.at/oesterreich.xml",
            "https://rss.orf.at/ooe.xml"};
    private boolean keepRunning = true;
    private boolean pauseScanning = false;
    private final SystemTray tray;
    private final TrayIcon trayIcon;
    private final PopupMenu popupMenu;


    public NewsTray() throws AWTException, IOException {
        //setup system tray
        tray = SystemTray.getSystemTray();

        URL url = new URL("https://www.orf.at/mojo/1_1/storyserver/common/ms-metro-icon.png");
        Image image = ImageIO.read(url);
        trayIcon = new TrayIcon(image, "News");
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

        mainLoop();
    }

    public void mainLoop() throws IOException {
        long updateTime = System.nanoTime();
        long delayTime = 60 * (long) Math.pow(10, 9);
        HashSet<String> previousHeadlines = new HashSet<>();
        ArrayList<String> headlines = new ArrayList<>();


        //get current headlines, add as known
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
            throw new IOException();
        } else {
            previousHeadlines.addAll(headlines);
        }


        //loop, if enough time as passed and scanning is active get new headlines
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
                        if (!previousHeadlines.contains(currentHeadline)) {
                            showMessage(currentHeadline);
                            System.out.println("found new Headline");
                            previousHeadlines.add(currentHeadline);
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
                        if (!previousHeadlines.contains(currentHeadline)) {
                            showMessage(currentHeadline);
                            System.out.println("found new Headline");
                            previousHeadlines.add(currentHeadline);
                        }
                    }
                    System.out.println("Getting headlines");
                }
                updateTime = updateTime + delayTime;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("Thread sleep interrupted. Interesting.");
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
        String fileName;
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
        ArrayList<String> headlines;
        headlines = getHeadlinesFromXML(filePath);
        return headlines;
    }

    public String getXMLFromRSSFeed(String targetURL) throws IOException {
        URL url = new URL(targetURL);
        String filePath = "pages//";
        String fileName;
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

            //text content of every (first) "title" child of all "item" nodes, should be sturdier this way
            NodeList itemList = document.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                headlines.add( ((Element) itemList.item(i)).getElementsByTagName("title").item(0).getTextContent() );
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            System.out.println("Failed To Parse XML");
        }
        return headlines;
    }

    public void showMessage(String message) {
        trayIcon.displayMessage("NEWS", message, TrayIcon.MessageType.NONE);
    }
}
