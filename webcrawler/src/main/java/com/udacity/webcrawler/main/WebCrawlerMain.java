package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;


import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;



public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)

      String rPath = config.getResultPath();

      try {
          if (rPath == null || rPath.isEmpty()) {
              try (PrintWriter printWriter = new PrintWriter(
                      new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)) {
                  resultWriter.write(printWriter);
              }
          } else {
              resultWriter.write(Path.of(rPath));
          }
      } catch (Exception ex) {
          System.err.println("Invalid:- error writing crawl results: " + ex.getMessage());
          ex.printStackTrace();
      }



      // TODO: Write the profile data to a text file (or System.out if the file name is empty)

      String pPath = config.getProfileOutputPath();
      if (pPath == null || pPath.isEmpty()) {
          try (PrintWriter printWriter = new PrintWriter(
                  new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)) {
              profiler.writeData(printWriter);
          }
      } else {
          profiler.writeData(Path.of(pPath));
      }


  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
