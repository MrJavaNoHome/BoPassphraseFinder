import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BoPassphraseFinder {
    private static final String MAIN_PAGE_URL = "http://www.bialystokonline.pl";
    private static final String PARTIES_URL = MAIN_PAGE_URL.concat("/imprezy");
    private static final String CONTESTS_URL = MAIN_PAGE_URL.concat("/konkursy.php");
    private static final String PASSPHRASE_PREFIX = "HASŁO DNIA: ";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
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
                String passphrase = null;
                for (String url : getTodayPartyUrls()) {
                    System.out.println("Checking url " + url);
                    if (findPassphraseElement(url).isPresent()) {
                        String passphraseElement = findPassphraseElement(url).get().text();
                        passphrase = passphraseElement.substring(passphraseElement.indexOf(PASSWORD_PREFIX)).substring(PASSPHRASE_PREFIX.length());
                        System.out.println(PASSPHRASE_PREFIX + passphrase);
                        break;
                    }
                }
                if (Objects.nonNull(passphrase)) {
                    for (String url : chosenContests) {
                        sendPasswordForContest(url, passphrase);
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


    private static List<String> getTodayPartyUrls() throws IOException {
        Document doc = Jsoup.connect(PARTIES_URL).get();
        List<String> partyCategoryUrls = doc.select("div.spis_imprez > a").stream().map(atr -> PARTIES_URL.concat(atr.attr("href"))).collect(Collectors.toList());
        List<String> partyUrls = new ArrayList<>();
        for (String partyCategoryUrl : partyCategoryUrls) {
            doc = Jsoup.connect(partyCategoryUrl).get();
            List<Element> partyElements = doc.select("div.item");
            for (Element partyElement : partyElements) {
                boolean todayParty = false;
                String dateS = partyElement.select("div.date").text();
                LocalDate now = LocalDate.now();
                //daty imprezy w formacie 2020-05-02
                if(dateS.length()==DATE_PATTERN.length()) {
                    LocalDate partyDate =  LocalDate.parse(dateS, DateTimeFormatter.ofPattern(DATE_PATTERN));
                    if (partyDate.isEqual(now)) {
                        todayParty = true;
                    }
                } else {  //daty imprezy w formacie 2020-05-02 - 2020-05-03
                    LocalDate dateFrom =  LocalDate.parse(dateS.substring(0, DATE_PATTERN.length()), DateTimeFormatter.ofPattern(DATE_PATTERN));
                    LocalDate dateTo =  LocalDate.parse(dateS.substring(dateS.length()-DATE_PATTERN.length()), DateTimeFormatter.ofPattern(DATE_PATTERN));
                    if((dateFrom.isEqual(now) || dateFrom.isBefore(now)) && (dateTo.isEqual(now) || dateTo.isAfter(now))) {
                        todayParty = true;
                    }
                }
                if(todayParty) {
                    partyUrls.add(PARTIES_URL.concat(partyElement.select("div.item > div.name > a, div.item2 > div.name > a").attr("href")));
                }
            }
        }
        System.out.println(String.format("Found %d urls to check", partyUrls.size()));
        return partyUrls;
    }


    private static Optional<Element> findPassphraseElement(String partyUrl) throws IOException {
        Document document = Jsoup.connect(partyUrl).get();
        return  document.select("div.opis_imprezy").stream().filter(a->a.text().toUpperCase().contains(PASSPHRASE_PREFIX)).findFirst();
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

