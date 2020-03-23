package oneandone.fileservice.server.cache;

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

import oneandone.fileservice.server.service.impl.NioFilesWrapper;

@Component
public class FileCounterCache implements InitializingBean {

    private long folderFileCount;

    @Value("${rootFolderPath}")
    private String rootFolderPath;

    @Autowired
    private NioFilesWrapper nioFilesWrapper;

    private static final Logger LOG = LoggerFactory.getLogger(FileCounterCache.class);

    @Override
    public void afterPropertiesSet() throws IOException {
        Path rootFolder = Paths.get(rootFolderPath).toAbsolutePath().normalize();

        DirectoryStream<Path> directoryStream = nioFilesWrapper.newDirectoryStream(rootFolder);

        directoryStream.forEach((path)-> folderFileCount++);

        LOG.info("There are currently {} files in {}",folderFileCount,rootFolderPath);
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
