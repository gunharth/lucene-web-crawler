package com.gunicode.lucene_web_crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashSet;

import org.apache.lucene.index.*;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.*;

public class Crawler {

    private int crawlDepth;
    private BufferedWriter bfWriter;
    private HashSet<String> indexedPages;

    public void startCrawl(String seedUrl, int crawlDepth, String indexPath) {

        this.crawlDepth = crawlDepth;

        IndexFiles indexer = new IndexFiles();
        IndexWriter writer = indexer.getIndexer(indexPath);

        // writer will be null when an index is already present in the index path
        if (writer == null) {
            System.out.println("Using Already Available Index...");
            return;
        }

        // create the log file
        try {
            bfWriter = new BufferedWriter(new FileWriter(indexPath + "/pages.txt"));
        } catch (IOException e) {
            System.out.println("Exception while creating pages.txt. " + e);
        }

        // initialize a HashSet to store pages that get indexed, so that
        // we can check if a page is already indexed before indexing.
        // HashSet is used because it allows for fast searching O(1).
        this.indexedPages = new HashSet<String>();

        // start the crawl procedure
        this.crawl(UrlNormalizer.normalize(seedUrl), 0, writer);

        // close index writer and bufferedWriter after crawling is done
        try {
            writer.close();
            bfWriter.close();
        } catch (IOException e) {
            System.out.println("Exception while closing index writer or buffered writer. " + e);
        }
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        } catch (KeyManagementException e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        }
    }

    private void crawl(String url, int depth, IndexWriter writer) {
        // if url is null after normalization
        if (url == null) {
            return;
        }

        // parse the document using jsoup
        org.jsoup.nodes.Document doc = null;
        try {
            Connection con = Jsoup.connect(url)
                    //.sslSocketFactory(this.socketFactory())
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36")
                    .ignoreHttpErrors(true)
                    .timeout(20000);
            Connection.Response response = con.execute();
            if (response.statusCode() == 200) {
                doc = con.get();
            }
        } catch (HttpStatusException e) {
            System.out.println("URL could not be parsed. " + e);
        } catch (Exception e) {
            System.out.println("Jsoup exception while connecting to url: " + url + ". " + e);
        }

        if (doc != null) {
            // index the current doc
            System.out.println("Adding " + url);
            // jsoup settings
            /* doc.select("form").remove(); // remove the form from the html
            doc.select("div#header").remove();
            doc.select("div#navigation").remove();
            doc.select("div.tab").remove();
            doc.select("div#simple").remove();
            doc.select("div#ext").remove();
            doc.select("div#info").remove();
            doc.select("div.dialogtext4").remove();
            doc.select("div#footer").remove();
            doc.select("canvas").remove();
            doc.select("script").remove(); */

            IndexFiles.indexDoc(writer, doc);

            // add the url to the indexedPages HashSet
            this.indexedPages.add(url);

            // write the current page to the log file pages.txt
            String line = url + "\t" + depth;
            try {
                bfWriter.write(line);
                bfWriter.newLine();
                bfWriter.flush();
            } catch (IOException e) {
                System.out.println("Error while writing to pages.txt. " + e);
            }

            // check if crawl depth has been reached
            if (depth < this.crawlDepth) {
                // extract links from the url and recurse
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String normalizedUrl = UrlNormalizer.normalize(link.absUrl("href").toString());
                    // recurse on the url if page is not already indexed
                    if (normalizedUrl != null && !this.indexedPages.contains(normalizedUrl)) {
                        crawl(normalizedUrl, depth+1, writer);
                    }
                }
            }
        }
    }
}
