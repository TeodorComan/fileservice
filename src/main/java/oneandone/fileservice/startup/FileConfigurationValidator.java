package oneandone.fileservice.startup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import oneandone.fileservice.server.exception.ServerException;
import oneandone.fileservice.server.service.impl.NioFilesWrapper;

@Component
public class FileConfigurationValidator implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(FileConfigurationValidator.class);

    @Value("${rootFolderPath}")
    String rootFolderPath;

    @Value("${backupFolderName}")
    String backupFolderName;

    @Autowired
    private NioFilesWrapper nioFilesWrapper;

    @Override
    public void afterPropertiesSet() {

        LOG.info("The store folder is: {}", rootFolderPath);
        Path rootFolder = Paths.get(this.rootFolderPath).toAbsolutePath().normalize();
        if(nioFilesWrapper.notExists(rootFolder) || !nioFilesWrapper.isDirectory(rootFolder)){
            throw new ServerException("The folder specified for saving the files doesn't exist");
        }

        LOG.info("The backup folder is: {}", backupFolderName);
        Path backupFolderPath = Paths.get(this.backupFolderName).toAbsolutePath().normalize();
        if(nioFilesWrapper.notExists(backupFolderPath) || !nioFilesWrapper.isDirectory(backupFolderPath)){
            throw new ServerException("The folder specified for backing up the files doesn't exist");
        }

    }
}
