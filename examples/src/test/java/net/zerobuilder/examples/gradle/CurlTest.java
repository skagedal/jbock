package net.zerobuilder.examples.gradle;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CurlTest {

  @Test
  public void testRemaining() throws Exception {
    Curl_Parser.Binder binder = Curl_Parser.parse(
        new String[]{"-H'Content-Type: application/json'", "-v", "http://localhost:8080"});
    Arrays.stream(Curl_Parser.Option.values())
        .map(o -> o.describe(2))
        .forEach(System.out::println);
    Curl curl = binder.bind();
    assertThat(curl.urls.size(), is(1));
    assertThat(curl.urls.get(0), is("http://localhost:8080"));
  }
}