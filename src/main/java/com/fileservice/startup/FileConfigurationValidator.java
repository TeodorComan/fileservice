package com.fileservice.startup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fileservice.server.exception.ServerException;
import com.fileservice.server.service.impl.NioFilesWrapper;

@Component
public class FileConfigurationValidator implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(FileConfigurationValidator.class);

    @Value("${fpath}")
    String path;

    @Value("${fname}")
    String name;

    @Autowired
    private NioFilesWrapper nioFilesWrapper;

    @Override
    public void afterPropertiesSet() {

        LOG.info("The store folder is: {}", path);
        Path rootFolder = Paths.get(this.path).toAbsolutePath().normalize();
        if(nioFilesWrapper.notExists(rootFolder) || !nioFilesWrapper.isDirectory(rootFolder)){
            throw new ServerException("The folder specified for saving the files doesn't exist");
        }

        LOG.info("The backup folder is: {}", name);
        Path backupFolderPath = Paths.get(this.name).toAbsolutePath().normalize();
        if(nioFilesWrapper.notExists(backupFolderPath) || !nioFilesWrapper.isDirectory(backupFolderPath)){
            throw new ServerException("The folder specified for backing up the files doesn't exist");
        }

    }
}
