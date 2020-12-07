package com.hiring.integration;


import com.hiring.CvIndexingApplication;
import com.hiring.services.ElasticService;
import com.hiring.util.TestRessources;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,classes = CvIndexingApplication.class)
@AutoConfigureMockMvc
public class MainRestControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private static ElasticService service;


    /**
     * Scenario de test dans le cas d'un ajout de cv pdf
     * @throws Exception
     */
    @Test
    public void addCV_Test_PDF() throws Exception {
        byte[] contents = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST));
        MockMultipartFile filex = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST, TestRessources.APPLICATION_PDF, contents);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex)).andExpect(status().is(201))
                .andExpect(content().string(new JSONObject().put("code","200").put("message","Added").toString()));
        mvc.perform(delete("/remove"));
    }

    /**
     * Scenario de test dans le cas d'un ajout de cv docx
     * @throws Exception
     */
    @Test
    public void addCV_Test_DOCX() throws Exception {
        byte[] contents = Files.readAllBytes(Paths.get("jonas cv.docx"));
        MockMultipartFile filex = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST_DOCX, TestRessources.APPLICATION_DOCX, contents);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex)).andExpect(status().is(201))
                .andExpect(content().string(new JSONObject().put("code","200").put("message","Added").toString()));
        mvc.perform(delete("/remove"));
    }

    /**
     * Scenario de test dans le cas d'une recherche d'un cv pdf
     * @throws Exception
     */
    @Test
    public void searchCV_PDF() throws Exception {
        byte[] contents = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST));
        MockMultipartFile filex = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST, TestRessources.APPLICATION_PDF, contents);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex));
        mvc.perform(get("/cv").param("keyword","Skander")).
                andExpect(content().string(new JSONArray().put(new JSONObject().put("originalName",TestRessources.ORIGINAL_NAME_TEST).put("contentType",TestRessources.APPLICATION_PDF)).toString()));
        mvc.perform(delete("/remove"));
    }

    /**
     * Scenario de test dans le cas d'une recherche d'un cv docx
     * @throws Exception
     */
    @Test
    public void searchCV_DOCX() throws Exception {
        byte[] contents = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST_DOCX));
        MockMultipartFile filex = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST_DOCX, TestRessources.APPLICATION_DOCX, contents);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex));
        mvc.perform(get("/cv").param("keyword","Dunfield")).
                andExpect(content().string(new JSONArray().put(new JSONObject().put("originalName",TestRessources.ORIGINAL_NAME_TEST_DOCX).put("contentType",TestRessources.APPLICATION_DOCX)).toString()));
        mvc.perform(delete("/remove"));
    }

    /**
     * Scenario de test dans le cas d'une recherche de cv's pdf et docx
     * @throws Exception
     */
    @Test
    public void searchCV_DOCX_PDF() throws Exception {
        byte[] contents1 = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST_DOCX));
        MockMultipartFile filex1 = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST_DOCX, TestRessources.APPLICATION_DOCX, contents1);

        byte[] contents2 = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST));
        MockMultipartFile filex2 = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST, TestRessources.APPLICATION_PDF, contents2);


        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex1));
        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex2));
        mvc.perform(get("/cv").param("keyword","Dunfield Java")).
                andExpect(content().string(new JSONArray().put(new JSONObject().put("originalName",TestRessources.ORIGINAL_NAME_TEST_DOCX).put("contentType",TestRessources.APPLICATION_DOCX))
                        .put(new JSONObject().put("originalName",TestRessources.ORIGINAL_NAME_TEST).put("contentType",TestRessources.APPLICATION_PDF)).toString()));
        mvc.perform(delete("/remove"));
    }

    /**
     * Scenario de test dans le cas d'un telechargement de cv.
     * @throws Exception
     */
    @Test
    public void downloadCV() throws Exception {
        String originalName =  TestRessources.ORIGINAL_NAME_TEST;

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mvc.perform(get("/downloadcv").param("fileName",TestRessources.ORIGINAL_NAME_TEST)).andExpect(status().isOk());
    }

    /**
     * Scenario de test dans le cas d'une suppression d'index.
     * @throws Exception
     */
    @Test
    public void removeIndex() throws Exception {

        byte[] contents = Files.readAllBytes(Paths.get(TestRessources.ORIGINAL_NAME_TEST));
        MockMultipartFile filex = new MockMultipartFile("file", TestRessources.ORIGINAL_NAME_TEST, TestRessources.APPLICATION_PDF, contents);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mvc.perform(MockMvcRequestBuilders.multipart("/addcv").file(filex)).andExpect(status().is(201))
                .andExpect(content().string(new JSONObject().put("code","200").put("message","Added").toString()));

        mvc.perform(delete("/remove")).andExpect(content().string(new JSONObject().put("code","201").put("message","removed succesfully").toString()));
    }
}
