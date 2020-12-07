package com.hiring.services;

import org.apache.http.HttpHost;
import org.apache.lucene.search.Query;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static com.hiring.util.TestRessources.*;

@Service
public class ElasticService {

    public static final String INDEX = "cvs";
    public static int id = 0;
    static RestHighLevelClient client;
    public ElasticService(){
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));
    }

    public static void addCv(String payload,String originalName, String contentType, RestHighLevelClient client_aux) throws IOException{
        client = client_aux;
        addCv(payload,originalName,contentType);
    }
    public static void addCv(String payload, String originalName, String contentType) throws IOException {
        System.out.println(payload);
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.field(PAYLOAD_FIELD, payload);
            builder.field(FILENAME_FIELD, originalName);
            builder.field(CONTENT_FIELD, contentType);
        }
        builder.endObject();
        id++;
        IndexRequest request = new IndexRequest(INDEX).id(Integer.toString(id)).source(builder).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
    }

    public static List<AbstractMap.SimpleImmutableEntry<String, String>> searchCV(String keyword) throws IOException{

        SearchSourceBuilder builder = new SearchSourceBuilder().searchSource();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.regexpQuery(PAYLOAD_FIELD,".*"+keyword.toLowerCase()+".*"));

        System.out.println("avant test "+keyword );
        if(keyword.contains("__")){
            String[] keywords = keyword.split("__");
            query = QueryBuilders.boolQuery()
                    .must(QueryBuilders.regexpQuery(PAYLOAD_FIELD,".*"+keywords[0].toLowerCase()+".*"));
            for(int i = 1;i< keywords.length;i++){
                query = query.must(QueryBuilders.regexpQuery(PAYLOAD_FIELD,".*"+keywords[i].toLowerCase()+".*"));
            }
        }else if(keyword.contains(" ")){
            String[] keywords = keyword.split(" ");
            query = QueryBuilders.boolQuery()
                    .should(QueryBuilders.regexpQuery(PAYLOAD_FIELD,".*"+keywords[0].toLowerCase()+".*"));
            for(int i = 1;i< keywords.length;i++){
                query = query.should(QueryBuilders.regexpQuery(PAYLOAD_FIELD,".*"+keywords[i].toLowerCase()+".*"));
            }
        }

        /*
        BoolQueryBuilder query = ;
        if(keyword.contains("&&")){
            String[] keywords = keyword.split("&&");
            if(keywords.length>1){
                query = QueryBuilders.boolQuery()
                        .must(
                                QueryBuilders.matchQuery(PAYLOAD_FIELD,keywords[0]));
                for (int i = 1;i< keywords.length;i++) {
                    query = query.must(
                                    QueryBuilders.matchQuery(PAYLOAD_FIELD,keywords[i]));
                }
            }
        }*/

        builder.query(query).size(10000);
        SearchRequest searchRequest = new SearchRequest(INDEX)
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .source(builder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = response.getHits().getHits();


        List<AbstractMap.SimpleImmutableEntry<String, String>> originalNames = new ArrayList<>();
        Arrays.stream(searchHits).forEach(hit -> {
            originalNames.add(new AbstractMap.SimpleImmutableEntry<>(hit.getSourceAsMap().get(FILENAME_FIELD).toString(),
                    hit.getSourceAsMap().get(CONTENT_FIELD).toString()));
        });
        return originalNames;
    }

    public static boolean isAlreadyAdded(String originalName) throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder().searchSource();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(
                        QueryBuilders.matchQuery(FILENAME_FIELD,originalName));
        builder.query(query).size(10000);
        SearchRequest searchRequest = new SearchRequest().searchType(SearchType.DFS_QUERY_THEN_FETCH).source(builder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = response.getHits().getHits();

        return searchHits.length>=1;
    }

    public static boolean deleteIndex() throws IOException {
        client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        GetIndexRequest request = new GetIndexRequest(INDEX);
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        return !exists;
    }
}
