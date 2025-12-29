package com.udacity.webcrawler.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;


import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public static CrawlerConfiguration load(Reader reader) throws IOException {
    Objects.requireNonNull(reader);

    ObjectMapper mapper = new ObjectMapper();
    mapper.getFactory().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    return mapper.readValue(reader, CrawlerConfiguration.class);
  }


  public CrawlerConfiguration load() throws IOException {
    try (Reader reader = Files.newBufferedReader(path)) {
      return ConfigurationLoader.load(reader);
    }
  }





  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) {
    try {
      return load(reader);
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse crawler configuration", e);
    }
  }

}
