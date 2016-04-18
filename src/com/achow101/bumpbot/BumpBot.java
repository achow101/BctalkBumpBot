package com.achow101.bumpbot;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Andy Chow on 4/17/2016.
 */
public class BumpBot extends Application {

    private int gridHeight = 0;
    private static DoBumps doBumps = new DoBumps();
    private static Thread t = new Thread(doBumps);

    public static void main(String[] args)
    {
        // Start do bumps thread
        t.start();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {

        // Set GUI Title
        primaryStage.setTitle("Bitcointalk Thread Bump Bot");

        // Create grid for entries
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // URL Heading lable
        Label urlLbl = new Label("Bitcointalk URL");
        grid.add(urlLbl, 0, 0);

        // New entry url textfield
        TextField urlTextField = new TextField();
        urlTextField.setPrefWidth(300);
        grid.add(urlTextField, 0, 1);
        gridHeight++;

        // Bump Text heading label
        Label bumpTextLbl = new Label("Bump Text");
        grid.add(bumpTextLbl, 1, 0);

        // New entry bump text textfield
        TextField bumpTextTextField = new TextField();
        bumpTextTextField.setPrefWidth(300);
        grid.add(bumpTextTextField, 1, 1);

        // Button to add entry
        Button addBtn = new Button("Add Bump Entry");
        grid.add(addBtn, 2, 1);

        // Error text
        Text errorText = new Text();
        grid.add(errorText, 1, 2);
        gridHeight += 2;

        // Populate the grid with pre-existing entries
        // Open a database connection
        // (create a new database if it doesn't exist yet):
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bumps.odb");
        EntityManager em = emf.createEntityManager();

        // Get the next thread to bump
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BumpEntry> qNextBump = cb.createQuery(BumpEntry.class);
        Root<BumpEntry> bump = qNextBump.from(BumpEntry.class);
        qNextBump.select(bump);
        TypedQuery<BumpEntry> query = em.createQuery(qNextBump);
        List<BumpEntry> bumpList = query.getResultList();
        for (BumpEntry entry : bumpList)
        {
            addEntryToGrid(entry, grid);
        }

        // Close the database connection:
        em.close();
        emf.close();

        // Add the entry when clicked
        addBtn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent e) {
                // Get the data from the form
                String url = urlTextField.getText();
                String bumpText = bumpTextTextField.getText();

                // Check the URL
                try {
                    if (!url.substring(0, 40).equals("https://bitcointalk.org/index.php?topic=")) {
                        errorText.setFill(Color.RED);
                        errorText.setText("Incorrect URL");
                        return;
                    }
                } catch (StringIndexOutOfBoundsException e1) {
                    errorText.setFill(Color.RED);
                    errorText.setText("Incorrect URL");
                    return;
                }

                // Trim the URL to just the topic
                String trimUrl = url.substring(0, 40);
                int dotIndex = url.indexOf(".", 40);
                trimUrl += url.substring(40, dotIndex);
                trimUrl += ".0";

                // Escape the bump text
                //escapeInput(bumpText);

                // Open a database connection
                // (create a new database if it doesn't exist yet):
                EntityManagerFactory emf = Persistence.createEntityManagerFactory("bumps.odb");
                EntityManager em = emf.createEntityManager();

                // Check that the thread is not already being bumped
                BumpEntry bEntry = em.find(BumpEntry.class, trimUrl);
                if (bEntry != null)
                {
                    errorText.setFill(Color.RED);
                    errorText.setText("Thread already being bumped");
                    return;
                }

                // Create the entry
                BumpEntry entry = new BumpEntry(trimUrl, bumpText, 0);

                // Check the date of the last post
                try {
                    // Get the first page of the thread
                    Document threadFirstPage = Jsoup.connect(url).get();

                    // Get the navpages elements
                    Element headerNavBar = threadFirstPage.select("div[id=bodyarea] > table[width=100%][cellspacing=0][cellpadding=0][border=0]").first();
                    Elements headerNavPages = headerNavBar.select("tr > td.middletext > a.navPages");

                    // Get the page if there is only one page of posts
                    if (headerNavPages.size() == 0) {
                        headerNavPages = headerNavBar.select("tr > td.middletext > b");
                    }

                    // Get the last page and calculate url number
                    Element lastPage = headerNavPages.last();
                    int pages = Integer.parseInt(lastPage.text());
                    int urlNum = (pages - 1) * 20;

                    // Create the URL for the last page
                    String lastPageUrl = url.substring(0, url.length() - 1);
                    lastPageUrl += urlNum;

                    // Get the last page of the thread
                    Document threadLastPage = Jsoup.connect(lastPageUrl).get();

                    // Get the last post in thread
                    Element postTable = threadLastPage.select("table[cellpadding=0][cellspacing=0][border=0][width=100%].bordercolor > tbody").first();
                    Element firstPost = postTable.select("tr").first();
                    String postClass = firstPost.className();
                    Elements posts = postTable.select("tr." + postClass);
                    Element lastPost = posts.last();

                    // Get the date of last post
                    Element headerAndPost = lastPost.select("td.td_headerandpost").first();
                    Element dateAndSubj = headerAndPost.select("table > tbody > tr > td[valign=middle]").get(1);
                    Element dateElem = dateAndSubj.select("div.smalltext").first();
                    String dateStr = dateElem.text();

                    // Parse date string and get unix timestamp
                    SimpleDateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy, hh:mm:ss a");
                    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date;
                    if (dateStr.contains("Today")) {
                        date = new Date();
                        String currentDateStr = fmt.format(date);
                        dateStr = dateStr.replace("Today at", currentDateStr.substring(0, currentDateStr.lastIndexOf(",") + 1));
                    }
                    date = fmt.parse(dateStr);
                    long unixtime = date.getTime() / 1000;
                    entry.setTime(unixtime);


                } catch (Exception e2) {
                    e2.printStackTrace();
                }

                // add entry to db
                em.getTransaction().begin();
                em.persist(entry);
                em.getTransaction().commit();

                // Notify the bumping thread
                synchronized (doBumps)
                {
                    doBumps.notify();
                }

                // Close the database connection:
                em.close();
                emf.close();

                // Add the entry to display
                addEntryToGrid(entry, grid);

                // Clear textfields
                urlTextField.clear();
                bumpTextTextField.clear();
            }
        });

        Scene scene = new Scene(grid, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void addEntryToGrid(BumpEntry entry, GridPane grid)
    {
        // Thread URL
        TextField entryUrlLbl = new TextField();
        entryUrlLbl.setText(entry.getUrl());
        entryUrlLbl.setEditable(false);

        // Bump Text
        TextField entryBumpTextLbl = new TextField();
        entryBumpTextLbl.setText(entry.getBumpText());
        entryBumpTextLbl.setEditable(false);

        // Delete button
        Button delBtn = new Button("Remove");
        final int row = gridHeight;
        delBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                // Open a database connection
                // (create a new database if it doesn't exist yet):
                EntityManagerFactory emf = Persistence.createEntityManagerFactory("bumps.odb");
                EntityManager em = emf.createEntityManager();

                // Get the entry to be deleted
                BumpEntry bumpEntry = em.find(BumpEntry.class, entry.getUrl());

                // Remove entry from db
                em.getTransaction().begin();
                em.remove(bumpEntry);
                em.getTransaction().commit();

                // Close the database connection:
                em.close();
                emf.close();

                // Clear this row's grid
                grid.getChildren().remove(entryUrlLbl);
                grid.getChildren().remove(entryBumpTextLbl);
                grid.getChildren().remove(delBtn);

                // Notify the bumping thread
                synchronized (doBumps)
                {
                    doBumps.notify();
                }
            }
        });

        // Add to display
        grid.add(entryUrlLbl, 0, gridHeight);
        grid.add(entryBumpTextLbl, 1, gridHeight);
        grid.add(delBtn, 2, gridHeight);
        gridHeight++;
    }

    @Override
    public void stop()
    {
        System.exit(0);
    }

    public String escapeInput(String input) {
        String[] characters = {"\"", "\\", "{", "}"};
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        String line = input;
        for (String test : characters) {
            line = line.replace(test, "\\" + test);
        }
        sb.append("\"");
        return sb.toString();
    }
}
