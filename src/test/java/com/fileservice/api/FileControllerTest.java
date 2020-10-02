package com.fileservice.api;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.fileservice.server.model.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fileservice.FileServiceApplication;
import com.fileservice.server.exception.ClientExceptionMessage;
import com.fileservice.server.exception.ServerException;
import com.fileservice.server.service.FileService;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = MOCK,
        classes = {
                FileServiceApplication.class
        })
@AutoConfigureMockMvc
public class FileControllerTest {

    @Autowired
    private MockMvc mvc;


    @MockBean
    private FileService fileService;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHeadFiles() throws Exception {

        when(fileService.count()).thenReturn(5l);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.head("/file-service/v1/files");

        mvc.perform(builder).andExpect(status().isOk()).andExpect(header().longValue("X-Total-Count",5l));
    }

    @Test
    public void testGetFileNotFound() throws Exception {

        when(fileService.get("fileName.png")).thenReturn(Optional.empty());

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.head("/file-service/v1/files/filename.png");

        mvc.perform(builder).andExpect(status().isNotFound());
    }

    @Test
    public void testGetFile() throws Exception {

        File file = new File();
        file.setName("fileName.png");
        file.setContent("content".getBytes());
        file.setLastModified(12345);

        when(fileService.get(file.getName())).thenReturn(Optional.of(file));

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/file-service/v1/files/"+file.getName());

        mvc.perform(builder).andExpect(status().isOk())
                            .andExpect(jsonPath("$.name").value("fileName.png"))
                            .andExpect(jsonPath("$.content").isNotEmpty())
                            .andExpect(jsonPath("$.lastModified").value(12345));
    }

    @Test
    public void testGetFileServerError() throws Exception {

        File file = new File();
        file.setName("fileName.png");
        file.setContent("content".getBytes());
        file.setLastModified(12345);

        when(fileService.get(file.getName())).thenThrow(new ServerException("Some Exception"));

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/file-service/v1/files/"+file.getName());

        mvc.perform(builder).andExpect(status().isBadRequest())
                .andExpect(content().string(ClientExceptionMessage.GENERAL_ERROR.toString()));
    }
}
