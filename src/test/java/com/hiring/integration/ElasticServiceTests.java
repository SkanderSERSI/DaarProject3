
package com.hiring.integration;

import com.hiring.services.ElasticService;
import com.hiring.util.TestRessources;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hiring.services.ElasticService.INDEX;
import static com.hiring.util.TestRessources.DOCKER_IMAGE_NAME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ElasticServiceTests {

    private static final Logger logger = LogManager.getLogger(ElasticServiceTests.class);
    private static RestHighLevelClient client;
    private static final String keyword = "Java";
    private static final String keyword_multiple = "Java Dunfield";
    private static final String keywordAND = "Java__Dunfield";
    private static ElasticsearchContainer container;

    @BeforeClass
    public static void startElasticsearchRestClient() throws IOException {
        int testClusterPort = Integer.parseInt(System.getProperty("tests.cluster.port", "9200"));
        String testClusterHost = System.getProperty("tests.cluster.host", InetAddress.getLocalHost().getHostAddress());
        String testClusterScheme = System.getProperty("tests.cluster.scheme", "http");
        String testClusterUser = System.getProperty("tests.cluster.user", "elastic");
        String testClusterPass = System.getProperty("tests.cluster.pass", "changeme");

        logger.info("Starting a client on {}://{}:{}", testClusterScheme, testClusterHost, testClusterPort);

        container = new ElasticsearchContainer(DOCKER_IMAGE_NAME);
        container.start();

        testClusterPort = container.getFirstMappedPort();

        // Demarrage du client
        RestClientBuilder builder = getClientBuilder(new HttpHost(testClusterHost, testClusterPort, testClusterScheme),
                testClusterUser, testClusterPass);
        client = new RestHighLevelClient(builder);

        MainResponse info = client.info(RequestOptions.DEFAULT);
        logger.info("Client is running against an elasticsearch cluster {}.", info.getVersion().toString());
    }

    @AfterClass
    public static void stopElasticsearchRestClient() throws IOException {
        if (client != null) {
            logger.info("Closing elasticsearch client.");
            client.close();
        }

        if(container != null){
            container.close();
        }
    }

    private static RestClientBuilder getClientBuilder(HttpHost host, String username, String password) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        return RestClient.builder(host)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }

    @Test
    public void testAdd_CV_PDF() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }
        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_PDF,TestRessources.ORIGINAL_NAME_TEST,TestRessources.APPLICATION_PDF,client);

        // We search
        SearchSourceBuilder builder = new SearchSourceBuilder().searchSource();
        MatchAllQueryBuilder query = QueryBuilders.matchAllQuery();
        builder.query(query).size(10000);
        SearchRequest searchRequest = new SearchRequest(INDEX);
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(builder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = response.getHits().getHits();
        logger.info("{}", response);

        System.out.println(response.getHits().getHits().length);

        assertThat(response.getHits().getTotalHits().value, is(1L));
    }


    @Test
    public void testSearch_CV_PDF_Scenario() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }

        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_PDF,TestRessources.ORIGINAL_NAME_TEST,TestRessources.APPLICATION_PDF,client);

        AbstractMap.SimpleImmutableEntry<String, String> result = ElasticService.searchCV(keyword).get(0);
        assertThat(result.getKey(), is(TestRessources.ORIGINAL_NAME_TEST));

    }

    @Test
    public void testSearch_CV_Scenario_multipleKeyword() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }

        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_PDF,TestRessources.ORIGINAL_NAME_TEST,TestRessources.APPLICATION_PDF,client);
        // Adding DOCX file
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_DOCX,TestRessources.ORIGINAL_NAME_TEST_DOCX,TestRessources.APPLICATION_DOCX,client);

        List<AbstractMap.SimpleImmutableEntry<String, String>> result = ElasticService.searchCV(keyword_multiple);
        assertThat(result.get(0).getKey(), is(TestRessources.ORIGINAL_NAME_TEST_DOCX));
        System.out.println(result.get(0).getKey());
        System.out.println(result.get(1).getKey());
        assertThat(result.get(1).getKey(), is(TestRessources.ORIGINAL_NAME_TEST));

    }



    // DOCX

    @Test
    public void testAdd_CV_WORD() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }
        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_DOCX,TestRessources.ORIGINAL_NAME_TEST_DOCX,TestRessources.APPLICATION_DOCX,client);

        // We search
        SearchSourceBuilder builder = new SearchSourceBuilder().searchSource();
        MatchAllQueryBuilder query = QueryBuilders.matchAllQuery();
        builder.query(query).size(10000);
        SearchRequest searchRequest = new SearchRequest(INDEX);
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(builder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = response.getHits().getHits();
        logger.info("{}", response);

        System.out.println(response.getHits().getHits().length);

        assertThat(response.getHits().getTotalHits().value, is(1L));
    }

    @Test
    public void testSearch_CV_DOCX_Scenario() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }

        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_DOCX,TestRessources.ORIGINAL_NAME_TEST_DOCX,TestRessources.APPLICATION_DOCX,client);

        List<AbstractMap.SimpleImmutableEntry<String, String>> result = ElasticService.searchCV(keyword);
        System.out.println(result);
        assertThat(result.get(0).getKey(), is(TestRessources.ORIGINAL_NAME_TEST_DOCX));

    }

    @Test
    public void testSearch_CV_KeywordAND_Scenario() throws IOException {
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }

        // Adding Skander's CV
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_DOCX,TestRessources.ORIGINAL_NAME_TEST_DOCX,TestRessources.APPLICATION_DOCX,client);
        ElasticService.addCv(TestRessources.PAYLOAD_TEST_PDF,TestRessources.ORIGINAL_NAME_TEST,TestRessources.APPLICATION_PDF,client);
        List<AbstractMap.SimpleImmutableEntry<String, String>> result = ElasticService.searchCV(keywordAND);
        System.out.println(result);
        assertThat(result.get(0).getKey(), is(TestRessources.ORIGINAL_NAME_TEST_DOCX));
    }

    @Test
    public void test_isAlreadyAdded() throws IOException{
        // We remove any existing index
        try {
            logger.info("-> Removing index {}.", INDEX);
            client.indices().delete(new DeleteIndexRequest(INDEX), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            assertThat(e.status().getStatus(), is(404));
        }

        ElasticService.addCv(TestRessources.PAYLOAD_TEST_PDF,TestRessources.ORIGINAL_NAME_TEST,TestRessources.APPLICATION_PDF,client);
        boolean res = ElasticService.isAlreadyAdded(TestRessources.ORIGINAL_NAME_TEST);
        assertThat(res, is(true));
    }

    @Test
    public void isDeleted() throws IOException {

        ElasticService.deleteIndex();
        GetIndexRequest request = new GetIndexRequest(INDEX);
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        assertThat(exists, is(false));
    }
}
