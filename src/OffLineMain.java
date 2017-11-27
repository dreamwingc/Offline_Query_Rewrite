import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OffLineMain {
    public static void main(String[] args) throws FileNotFoundException{
        String synonymsPath = args[0];
        String queryPath = args[1];
        String memcachedServer = args[2];
        String memcachedPortal = args[3];
        String address = memcachedServer + ":" + memcachedPortal;
        MemcachedClient cache = null;
        int EXP = 0;

        HashMap<String, List<String>> synonymsDict = new HashMap<>();
        List<String> queryTerm = new ArrayList<>();

        List<String> synonymsList = OpenFile(synonymsPath);
        List<String> queryFile = OpenFile(queryPath);

        //memchached
        try
        {
            cache = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), AddrUtil.getAddresses(address));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // process Json data to ArrayList in synonyms file
        for (String sterm: synonymsList){
            JSONObject jsonObject = new JSONObject(sterm);
            JSONArray slist = (JSONArray) jsonObject.get("synonyms");

            String tempStr = slist.toString().replaceAll("[\\[\\]\"]", "");
            String[] temp = tempStr.split(",");

            List<String> subList = new ArrayList<>();

            for(String sub:temp){
                subList.add(sub);
            }
            subList.add(jsonObject.getString(("word")));
            //System.out.println(subList);
            synonymsDict.put(jsonObject.getString("word"), subList);
        }

        for(String qterm: queryFile){
            JSONObject jsonObject = new JSONObject(qterm);
            String query = jsonObject.getString("query");

            query.replaceAll("[\"]", "");
            query.toLowerCase();
            String[] querySplit = query.split(" ");

            List<String> qsubList = new ArrayList<>();

            for(String squery:querySplit){
                qsubList.add(squery);
            }

            String queryKey = Utility.strJoin(qsubList, "_");
            System.out.println(queryKey);

            QueryParser queryParser = new QueryParser();
            List<List<String>> result = queryParser.OfflineQueryRewrite(qsubList, synonymsDict, memcachedServer, Integer.valueOf(memcachedPortal));

            cache.set(queryKey, EXP, result);
        }
    }

    private static List<String> OpenFile(String path) throws FileNotFoundException{

        List<String> result = new ArrayList<>();

        FileReader fileReader = new FileReader(path);
        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String curr;

            while((curr = bufferedReader.readLine())!=null)
            {
                result.add(curr);
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        return result;
    }
}
