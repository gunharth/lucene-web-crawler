package com.gunicode.lucene_web_crawler;

public class Main
{
    public static void main( String[] args )
    {
        if (!args[0].equals("") && !args[1].equals("") && !args[2].equals("") && !args[3].equals("")) {

            // Get seedUrl, crawlDepth, index path, and search query from cmd line arguments
            String seedUrl = args[0];
            int crawlDepth = Integer.parseInt(args[1]);
            String indexPath = args[2];
            String query = args[3];

            // create a crawler object and call the startCrawl method by passing in
            // the seedUrl, crawlDepth, and index path
            Crawler crawler = new Crawler();
            crawler.startCrawl(seedUrl, crawlDepth, indexPath);

            SearchFiles.search(indexPath, query);
        }
        else {
            System.out.println("Invalid command line arguments. Must be run as follows:\n");
            System.out.println("[seed URL] [crawl depth] [path to index folder] [query]\n");
        }
    }
}