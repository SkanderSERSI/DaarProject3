package com.hiring.controller;


import com.hiring.services.ElasticService;
import com.hiring.util.TestRessources;
import com.sun.xml.internal.ws.api.pipe.ContentType;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;

@RestController
public class MainController {

    @Autowired
    protected static ElasticService elasticService;
    protected final static String APPLICATION_PDF  = "application/pdf";
    protected final static String APPLICATION_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    protected final static String APPLICATION_DOC  = "application/msword";

    @PostMapping("/addcv")
    @ResponseStatus(HttpStatus.CREATED)
    public String uploadCV(@RequestParam("file") MultipartFile multiPartFile) throws IOException, InvalidFormatException {
        String response;
        File file = new File(multiPartFile.getOriginalFilename());
        file.createNewFile();
        FileOutputStream fout = new FileOutputStream(file);
        fout.write(multiPartFile.getBytes());
        fout.close();
        System.out.println(multiPartFile.getContentType());
        if(!elasticService.isAlreadyAdded(multiPartFile.getOriginalFilename())){
            if(multiPartFile.getContentType().equals(APPLICATION_PDF)){
                PDFParser pdfParser = new PDFParser(new RandomAccessBufferedFileInputStream(file));
                pdfParser.parse();
                try (COSDocument cos = pdfParser.getDocument();
                     PDDocument pd = new PDDocument(cos)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(1);
                    stripper.setEndPage(pd.getNumberOfPages());
                    elasticService.addCv(stripper.getText(pd),multiPartFile.getOriginalFilename(),APPLICATION_PDF);
                }
            }else{
                FileInputStream filex = new FileInputStream(multiPartFile.getOriginalFilename());
                XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(filex));
                XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
                System.out.println(extractor.getText());
                elasticService.addCv(extractor.getText(),multiPartFile.getOriginalFilename(),APPLICATION_DOCX);
            }
            response = new JSONObject().put("code","200").put("message","Added").toString();
        }else{
            response = new JSONObject().put("code","409").put("message","Duplicated CV").toString();
        }

        return response;
    }

    @GetMapping("/cv")
    public String searchCV(@RequestParam("keyword") String keyword) throws IOException {
        List<AbstractMap.SimpleImmutableEntry<String, String>> hits = elasticService.searchCV(keyword);
        System.out.println("Number of hits : "+hits.size());
        JSONArray response = new JSONArray();
        for (AbstractMap.SimpleImmutableEntry<String, String> hit:hits){
            JSONObject jo = new JSONObject();
            jo.put("originalName", hit.getKey());
            jo.put("contentType", hit.getValue());
            response.put(jo);
        }
        if(response.isEmpty()){
            response= new JSONArray().put(new JSONObject().put("code","404").put("message","Not found"));
        }

        return response.toString();
    }

    @GetMapping("/downloadcv")
    public ResponseEntity<byte[]> download(@RequestParam("fileName") String originalName) throws IOException {
        byte[] contents = Files.readAllBytes(Paths.get(originalName));
        boolean hasPdf = originalName.endsWith("pdf");
        String contentType ;
        if (hasPdf)
            contentType = TestRessources.APPLICATION_PDF;
        else
            contentType = TestRessources.APPLICATION_DOCX;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setContentDispositionFormData(originalName, originalName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        ResponseEntity<byte[]> response = new ResponseEntity<>(contents, headers, HttpStatus.OK);
        return response;
    }
    @DeleteMapping("/remove")
    public static String remove() throws IOException {
        JSONObject reponse = new JSONObject();
        if(elasticService.deleteIndex()){
            reponse.put("code","201").put("message","removed succesfully");
        }else{
            reponse.put("code","409").put("message","cant remove");
        }
        return reponse.toString();
    }
}
