import Entity.Medicine;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

/**
 * @author Created by swike <swikecc@gmail.com> on 2017/11/9.
 */
public class Main {
    public static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36";
    public static Map<String, String> cookies = new HashMap<>();
    public static Stack<Medicine> medicineStack = new Stack<>();
    public static Thread writerThread;
    public static boolean isEnd = false;
    public static long gap = 2000;

    static {
        // cookies configuration.
        cookies.put("think_language", "en-US");
        cookies.put("PHPSESSID", "pba4o286pgnpuvesvbm06s9go2");
        cookies.put("yaozh_logintime", "1510215231");
        cookies.put("yaozh_user", "508269%09%E6%B8%85%E6%96%B0%E5%8F%AF");
        cookies.put("db_w_auth", "480197%09%E6%B8%85%E6%96%B0%E5%8F%AF");
        cookies.put("UtzD_f52b_saltkey", "QVKS3S7v");
        cookies.put("UtzD_f52b_lastvisit", "1510211632");
        cookies.put("UtzD_f52b_lastact", "1510215232%09uc.php%09");
        cookies.put("UtzD_f52b_auth", "fbd8HqXAP6bCIYCM8nzxw9DofK2mujxuPUK%2BPS5eGIHGsqEE4Q8pTbNDeAKv3xd5xQ%2BHOaZsfQ601yB2OmP9b9FNjjU");
        cookies.put("_ga", "GA1.2.2142941406.1510215222");
        cookies.put("_gid", "GA1.2.1474210339.1510215222");
        cookies.put("WAF_SESSION_ID", "1d856a619e5a99270b9cdb6b28f73df5");
        cookies.put("_gat", "1");
        cookies.put("Hm_lvt_65968db3ac154c3089d7f9a4cbb98c94", "1510215221");
        cookies.put("Hm_lpvt_65968db3ac154c3089d7f9a4cbb98c94", "1510215268");

        // initial writer thread.
        writerThread = new Thread() {
            @Override
            public void run() {
                BufferedWriter writer = null;
                String fileName = "medicines.txt";
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
                    // loop write data.
                    while (true) {
                        if (!output(writer))
                            break;
                        // config the gap of writing.
                        Thread.sleep(gap / 10);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (writer != null) {
                            writer.flush();
                            writer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * writer function
     * @param fout
     * @return
     */
    public static boolean output(BufferedWriter fout) {
        synchronized (medicineStack) {
            boolean flag = true;
            if (!medicineStack.isEmpty()) {
                try {
                    Medicine medicine = medicineStack.pop();
                    fout.write(medicine.toString() + "\n");
                    System.out.println(medicine.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                flag = true;
            }
            // when medicine stack is empty and has been over.
            else if (isEnd)
                flag = false;
            return flag;
        }
    }

    /**
     * function to get the range of the wanted pages.
     * @return
     */
    public static int[] getRange() {
        int[] params = new int[2];
        params[0] = Integer.MAX_VALUE;
        params[1] = Integer.MIN_VALUE;
        for (int p = 1; p < 8; p++) {
            String url = "https://db.yaozh.com/chufang?p="+ p +"&pageSize=30";
            Document doc = null;
            try {
                doc = Jsoup.connect(url).userAgent(userAgent).cookies(cookies).timeout((int) gap * 10)
                        .referrer(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements elements = doc.select("table.table tbody").last().select("th > a");
            for (Element element : elements) {
                int linkNum = Integer.parseInt(element.attr("href").substring(9, 15));
                if(linkNum < params[0])
                    params[0] = linkNum;
                if(linkNum > params[1])
                    params[1] = linkNum;
            }
            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return params;
    }

    /**
     * function to get the specific page.
     * @param index
     */
    public static void spiderOne(int index) {
        String url = "https://db.yaozh.com/chufang/"+ index +".html";
        System.out.println(url);
        Document doc = null;
        while (doc == null) {
            try {
                doc = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .cookies(cookies)
                        .timeout((int) gap * 10)
                        .referrer("https://db.yaozh.com/chufang").get();
            } catch (IOException e) {
                if (e instanceof HttpStatusException) {
                    System.err.println(e.toString());
                    System.out.println("Please waiting for 20 minutes");
                    try {
                        Thread.sleep(1000 * 60 * 5);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        Elements names = doc.select("table.table th");
        if (names.isEmpty())
            return;
        List<String> nameList = new ArrayList<>();
        for (Element name : names) {
            String text = name.text();
            if (StringUtils.isNotBlank(text)) {
                nameList.add(text);
            }
        }

        Elements values = doc.select("table.table span");
        List<String> valueList = new ArrayList<>();
        for (Element value : values) {
            String text = value.text();
            if (StringUtils.isNotBlank(text)) {
                valueList.add(text);
            }
        }

        // add the medicine
        Medicine medicine = new Medicine(nameList, valueList);
        // synchronize the stack of medicines.
        synchronized (medicineStack) {
            medicineStack.add(medicine);
        }
    }

    public static void main(String[] args) {
        // start the thread of writing.
        writerThread.start();

//        int[] params = getRange();
//        System.out.println(params[0] + ":" + params[1]);
        int[] params = {100012, 124500};
        for (int i = params[0]; i <= params[1]; i++) {
            spiderOne(i);
            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // is end
        isEnd = true;
    }
}