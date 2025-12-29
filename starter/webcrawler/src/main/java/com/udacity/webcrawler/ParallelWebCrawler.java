package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.WordCounts;


import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {

  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final ForkJoinPool pool;

  @Inject
  private Provider<PageParserFactory> parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @TargetParallelism int threadCount) {

    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);

    Map<String, Integer> wordCounts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    class CrawlTask extends RecursiveAction {
      private final String url;
      private final int depth;

      CrawlTask(String url, int depth) {
        this.url = url;
        this.depth = depth;
      }

      @Override
      protected void compute() {
        // === EXACT MATCH TO SequentialWebCrawler ===
        if (depth == 0 || clock.instant().isAfter(deadline)) {
          return;
        }

        for (Pattern pattern : ignoredUrls) {
          if (pattern.matcher(url).matches()) {
            return;
          }
        }

        if (!visitedUrls.add(url)) {
          return;
        }

        PageParser.Result result =
                parserFactory.get().get(url).parse();

        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
          wordCounts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        invokeAll(
                result.getLinks().stream()
                        .map(link -> new CrawlTask(link, depth - 1))
                        .collect(Collectors.toList()));
      }
    }

    pool.invoke(new RecursiveAction() {
      @Override
      protected void compute() {
        invokeAll(
                startingUrls.stream()
                        .map(url -> new CrawlTask(url, maxDepth))
                        .collect(Collectors.toList()));
      }
    });

    if (wordCounts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(wordCounts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }



    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
