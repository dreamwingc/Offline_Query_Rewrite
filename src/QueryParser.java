import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import net.spy.memcached.MemcachedClient;


public class QueryParser {
    private static QueryParser instance = null;

    protected QueryParser() {

    }
    public static QueryParser getInstance() {
        if(instance == null) {
            instance = new QueryParser();
        }
        return instance;
    }
    public List<String> QueryUnderstand(String query) {
        List<String> tokens = Utility.cleanedTokenize(query);
        return tokens;
    }

    //nike running shoes
    private void QueryRewriteHelper(int index, int len, ArrayList<String> queryTermsTemp,List<List<String>> allSynonymList, List<List<String>> res) {
        if(index == len) {
            res.add(queryTermsTemp);
            return;
        }
        List<String> synonyms = allSynonymList.get(index);
        for(int i = 0; i < synonyms.size();i++) {
            ArrayList<String> queryTerms = (ArrayList<String>) queryTermsTemp.clone();
            queryTerms.add(synonyms.get(i));
            QueryRewriteHelper(index + 1,len,queryTerms,allSynonymList,res);
        }
    }
    //construct query rewrite online
    public List<List<String>> OfflineQueryRewrite(List<String> queryTerms, HashMap<String, List<String>> synonymsDict, String memcachedServer, int synonymMemcachedPortal) {
        List<List<String>> res = new ArrayList<List<String>>();
        List<List<String>> resTemp = new ArrayList<List<String>>();
        List<List<String>> allSynonymList = new ArrayList<List<String>>();
        try {
            for(String queryTerm:queryTerms) {
                if(synonymsDict.containsKey(queryTerm)) {
                    List<String>  synonymList = synonymsDict.get(queryTerm);
                    allSynonymList.add(synonymList);
                } else {
                    List<String>  synonymList = new ArrayList<String>();
                    synonymList.add(queryTerm);
                    allSynonymList.add(synonymList);
                }
            }
            int len = queryTerms.size();
            System.out.println("len of queryTerms = " + len);
            ArrayList<String> queryTermsTemp = new ArrayList<String>();
            QueryRewriteHelper(0, len, queryTermsTemp,allSynonymList,resTemp);

            //dedupe
            Set<String> uniquueQuery = new HashSet<String>();
            for(int i = 0;i < resTemp.size();i++) {
                String hash = Utility.strJoin(resTemp.get(i), "_");
                if(uniquueQuery.contains(hash)) {
                    continue;
                }
                uniquueQuery.add(hash);
                Set<String> uniquueTerm = new HashSet<String>();
                for(int j = 0;j < resTemp.get(i).size();j++) {
                    String term = resTemp.get(i).get(j);
                    if(uniquueTerm.contains(term)) {
                        break;
                    }
                    uniquueTerm.add(term);
                }
                if (uniquueTerm.size() == len) {
                    res.add(resTemp.get(i));
                }
            }
            //debug
            /*for(int i = 0;i < res.size();i++) {
                System.out.println("synonym");
                for(int j = 0;j < res.get(i).size();j++) {
                    System.out.println("query term = " + res.get(i).get(j));
                }
            }*/

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return res;
    }
}