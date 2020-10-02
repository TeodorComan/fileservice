package com.fileservice.server.cache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fileservice.server.service.impl.NioFilesWrapper;

@Component
public class FCCache implements InitializingBean {

    private long folderFileCount;

    @Value("${fpath}")
    private String path;

    @Autowired
    private NioFilesWrapper nioFilesWrapper;

    private static final Logger LOG = LoggerFactory.getLogger(FCCache.class);

    @Override
    public void afterPropertiesSet() throws IOException {
        Path rootFolder = Paths.get(path).toAbsolutePath().normalize();

        DirectoryStream<Path> directoryStream = nioFilesWrapper.newDirectoryStream(rootFolder);

        directoryStream.forEach((path)-> folderFileCount++);

        LOG.info("There are currently {} files in {}",folderFileCount, path);
    }

    public long getFolderFileCount() {
        return folderFileCount;
    }

    public void incrementFolderFileCount(){
        this.folderFileCount++;
        LOG.debug("Incremented file counter to: {}", folderFileCount);
    }

    public void decrementFolderFileCount(){
        this.folderFileCount--;
        LOG.debug("Decremented file counter to: {}",folderFileCount);
    }
}
