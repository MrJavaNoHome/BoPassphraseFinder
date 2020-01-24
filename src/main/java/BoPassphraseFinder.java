import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BoPassphraseFinder {
    private static final String MAIN_PAGE_URL = "http://www.bialystokonline.pl";
    private static final String PARTIES_URL = MAIN_PAGE_URL.concat("/imprezy");
    private static final String CLUB_PARTIES_URL = MAIN_PAGE_URL.concat("/imprezy-klubowe-taneczne-granie-do-piwa,imprezy,1,1.html");
    private static final String CONTESTS_URL = MAIN_PAGE_URL.concat("/konkursy.php");
    private static final String PASSPHRASE_PREFIX = "KONKURSOWE HASŁO DNIA: ";

    private static final String PRIZES_PREFIX = "Nagrody";
    private static final String PASSWORD_PREFIX = "Hasło";

    private static final String USERNAME = "xxx";
    private static final String USER_PASSWORD = "xxx";

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> openContestUrls = findOpenContests();
        if (!openContestUrls.isEmpty()) {
            List<Integer> chosenNumbers = getChosenNumbers(openContestUrls.size());
            if (!chosenNumbers.isEmpty()) {
                System.out.println("Chosen numbers: " + chosenNumbers);
                List<String> chosenContests = new ArrayList<>();
                for (Integer chosenNumber : chosenNumbers) {
                    chosenContests.add(openContestUrls.get(chosenNumber - 1));
                }
                String passphraseElement = null;
                for (String partyUrl : getClubPartyUrls()) {
                    System.out.println("Checking url " + partyUrl);
                    if (getPassphraseElement(partyUrl).isPresent()) {
                        passphraseElement = getPassphraseElement(partyUrl).get().text().substring(PASSPHRASE_PREFIX.length());
                        System.out.println(PASSPHRASE_PREFIX + passphraseElement);
                        break;
                    }
                }
                if (Objects.nonNull(passphraseElement)) {
                    for (String url : chosenContests) {
                        sendPasswordForContest(url, passphraseElement);
                        TimeUnit.SECONDS.sleep(2);
                    }
                }
            }
        }
        System.out.println("Program finished");
    }

    private static List<Integer> getChosenNumbers(int size) {
        System.out.println("Type numbers of contests (separated by ENTER) and type 0");
        boolean typingMode = true;
        Scanner sc = new Scanner(System.in);
        List<Integer> chosenNumbers = new ArrayList<>();
        try {
            while (typingMode) {
                int number = sc.nextInt();
                if (number == 0) {
                    typingMode = false;
                } else if (number <= size) {
                    chosenNumbers.add(number);
                }
            }
        } catch (Exception e) {
            System.out.println("Bad value");
        }
        return chosenNumbers;
    }

    private static List<String> findOpenContests() throws IOException {
        Document doc = Jsoup.connect(CONTESTS_URL).get();
        List<String> contestUrls =
                doc.select("div.konkurs > a").stream().map(atr -> MAIN_PAGE_URL.concat(atr.attr("href"))).collect(Collectors.toList());
        System.out.println(String.format("Found %d contests", contestUrls.size()));
        List<String> openContestUrls = new ArrayList<>();
        System.out.println("... and open are : ");
        for (String url : contestUrls) {
            Document contestDoc = Jsoup.connect(url).get();
            Element loginFormInOpenContest = contestDoc.selectFirst("div.form_login_ping");
            if (Objects.nonNull(loginFormInOpenContest)) {
                openContestUrls.add(url);
                System.out.print(openContestUrls.size() + ". ");
                String contestDescription = contestDoc.select("div.konkurs").text();
                int startIndex = contestDescription.indexOf(PRIZES_PREFIX);
                int endIndex = contestDescription.indexOf(PASSWORD_PREFIX);
                String prizeDescription = contestDescription.substring(startIndex, endIndex);
                System.out.println(prizeDescription);
            }
        }
        return openContestUrls;
    }


    private static Optional<Element> getPassphraseElement(String partyUrl) throws IOException {
        Document document = Jsoup.connect(partyUrl).get();
        return document.select("i").stream().filter(i -> i.text().toUpperCase().startsWith(PASSPHRASE_PREFIX)).findFirst();
    }

    private static List<String> getClubPartyUrls() throws IOException {
        Document doc = Jsoup.connect(CLUB_PARTIES_URL).get();
        List<String> urls =
                doc.select("div.item > div.name > a, div.item2 > div.name > a").stream().map(atr -> PARTIES_URL.concat(atr.attr("href"))).collect(Collectors.toList());
        System.out.println(String.format("Found %d urls to check", urls.size()));
        return urls;
    }

    private static void sendPasswordForContest(String url, String passphraseElement) throws IOException {
        Document doc = Jsoup.connect(url).get();
        String name = doc.select("div.border2_form").select("input[name=nazwa]").attr("value");
        String id = doc.select("div.border2_form").select("input[name=id]").attr("value");
        String submit = doc.select("div.border2_form").select("input[name=submit]").attr("value");
        Point coordinates = randomCoordinates();
        System.out.println("Sending password for contest: " + name);

        Document post = Jsoup.connect(CONTESTS_URL)
                .data("uzytkownik", USERNAME)
                .data("haslo", USER_PASSWORD)
                .data("odpowiedz1", passphraseElement)
                .data("nazwa", name)
                .data("id", id)
                .data("submit", submit)
                .data("x", String.valueOf(coordinates.x))
                .data("y", String.valueOf(coordinates.y))
                .post();
        System.out.println(post.select("div.ico_alert").first().text());

    }

    private static Point randomCoordinates() {
        Random rand = new Random();
        int x = rand.nextInt(185) + 1;
        int y = rand.nextInt(28) + 1;
        return new Point(x, y);
    }


}
