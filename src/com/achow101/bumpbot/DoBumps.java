/*
 * Copyright (C) 2016  Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.achow101.bumpbot;

import com.achow101.bumpbot.BumpEntry;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.List;

/**
 * Created by Andy Chow on 4/16/2016.
 */
public class DoBumps implements Runnable {

    private String ACCOUNT_NAME = "NOT THE RIGHT ACCOUNT";
    private String ACCOUNT_PASS = "NOT THE RIGHT PASSWORD";

    @Override
    public void run() {
        // Open a database connection
        // (create a new database if it doesn't exist yet):
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bumps.odb");
        EntityManager em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Do until program stop
        synchronized (this) {
            while (true) {
                // Get the next thread to bump
                CriteriaQuery<BumpEntry> qNextBump = cb.createQuery(BumpEntry.class);
                Root<BumpEntry> bump = qNextBump.from(BumpEntry.class);
                qNextBump.select(bump);
                TypedQuery<BumpEntry> query = em.createQuery(qNextBump);
                List<BumpEntry> nextBumpList = query.getResultList();

                // Wait if nextBump is null
                if (nextBumpList == null || nextBumpList.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // Find the next post to bump
                long minTime = nextBumpList.get(0).getTime();
                int inx = 0;
                int minInx = 0;
                for (BumpEntry bumpEntry : nextBumpList) {
                    if (minTime > bumpEntry.getTime()) {
                        minTime = bumpEntry.getTime();
                        minInx = inx;
                    }
                    inx++;
                }
                BumpEntry nextBump = nextBumpList.get(minInx);

                long origTime = nextBump.getTime();
                long waitFor = ((origTime + 84000) - (System.currentTimeMillis() / 1000)) * 1000;

                // Wait
                if(waitFor > 0) {
                    try {
                        wait(waitFor);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // Do the bump
                try {
                    // Login
                    Connection.Response res;

                    res = Jsoup.connect("https://bitcointalk.org/index.php?action=login2")
                            .followRedirects(true)
                            .data("user", ACCOUNT_NAME)
                            .data("passwrd", ACCOUNT_PASS)
                            .data("cookielength", "-1")
                            .method(Connection.Method.POST)
                            .execute();
                    Document loggedInDocument = res.parse();

                    String sessId = res.cookie("PHPSESSID");

                    // Get the thread
                    Document thread = Jsoup.connect(nextBump.getUrl()).cookie("PHPSESSID", sessId).get();

                    // Get the reply URL
                    Element replyUrlElem = thread.select("body > div[id=bodyarea] > form > table > tbody > tr > td[align=right] > table > tbody > tr> td.maintab_back > a").first();
                    if (replyUrlElem.text().equals("Reply")) {

                        // Get post data
                        Document replyPage = Jsoup.connect(replyUrlElem.attr("href")).cookie("PHPSESSID", sessId).get();
                        String sc = replyPage.select("input[type=hidden][name=sc]").first().attr("value");
                        String id = replyPage.select("td.windowbg > input[name=topic]").first().attr("value");
                        String subject = replyPage.select("td > input[name=subject]").first().attr("value");
                        String message = nextBump.getBumpText();

                        // Get post URL
                        String postURL = replyPage.select("Form[name=postmodify]").first().attr("action");

                        // Make post
                        res = Jsoup.connect(postURL)
                                .data("sc", sc)
                                .data("subject", subject)
                                .data("message", message)
                                .data("topic", id)
                                .data("icon", "xx")
                                .cookie("PHPSESSID", sessId)
                                .method(Connection.Method.POST)
                                .execute();
                    }

                    // Update the database entry with new date
                    em.getTransaction().begin();
                    nextBump.setTime(System.currentTimeMillis() / 1000);
                    em.getTransaction().commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
