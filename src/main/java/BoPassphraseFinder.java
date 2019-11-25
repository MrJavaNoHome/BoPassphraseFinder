import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BoPassphraseFinder {
   private static final String PARTIES_MAIN_PAGE_URL = "http://www.bialystokonline.pl/imprezy";
   private static final String CLUB_PARTIES_MAIN_PAGE_URL = "http://www.bialystokonline.pl/imprezy-klubowe-taneczne-granie-do-piwa,imprezy,1,1.html";
   private static final String PASSPHRASE_PREFIX = "KONKURSOWE HASŁO DNIA";

   public static void main(String[] args) throws IOException {
      for (String partyUrl : getPartyUrls()) {
         System.out.println("Checking url " + partyUrl);
         if (getPassphraseElement(partyUrl).isPresent()) {
            System.out.println(getPassphraseElement(partyUrl).get().text());
            break;
         }
      }
      System.out.println("Program finished");
   }

   private static Optional<Element> getPassphraseElement(String partyUrl) throws IOException {
      Document document = Jsoup.connect(partyUrl).get();
      return document.select("i").stream().filter(i -> i.text().toUpperCase().startsWith(PASSPHRASE_PREFIX)).findFirst();
   }

   private static List<String> getPartyUrls() throws IOException {
      Document doc = Jsoup.connect(CLUB_PARTIES_MAIN_PAGE_URL).get();
      List<String> urls =
            doc.select("div.item > div.name > a").stream().map(atr -> PARTIES_MAIN_PAGE_URL.concat(atr.attr("href"))).collect(Collectors.toList());
      System.out.println(String.format("Found %d urls to check", urls.size()));
      return urls;
   }

}
