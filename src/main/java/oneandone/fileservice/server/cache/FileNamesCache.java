package oneandone.fileservice.server.cache;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import oneandone.fileservice.server.service.impl.NioFilesWrapper;

@Component
public class FileNamesCache implements InitializingBean {

    @Value("${rootFolderPath}")
    private String rootFolderPath;

    @Autowired
    private NioFilesWrapper nioFilesWrapper;

    private List<CharArray> fileNames = new LinkedList<>();

    private static final Logger LOG = LoggerFactory.getLogger(FileNamesCache.class);

    public List<CharArray> getFileNames() {
        return fileNames;
    }

    public void addFileName(String fileName){
        LOG.debug("Caching file name: {}",fileName);
        fileNames.add(getCharArrayFromString(fileName));
    }

    public void removeFileName(String fileName){
        LOG.debug("Removing file name {} from cache",fileName);
        fileNames.remove(getCharArrayFromString(fileName));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        LOG.info("Initializing File Name Cache");

        Path rootFolder = Paths.get(rootFolderPath).toAbsolutePath().normalize();

        DirectoryStream<Path> directoryStream = nioFilesWrapper.newDirectoryStream(rootFolder);

        directoryStream.forEach((path)-> this.fileNames.add(getCharArrayFromString(path.getFileName().toString())));

        LOG.info("File Name Cache Initialization Complete");
    }

    private CharArray getCharArrayFromString(String s){

        char[] ch = new char[s.length()];

        for (int i = 0; i < s.length(); i++) {
            ch[i] = s.charAt(i);
        }

        return new CharArray(ch,0,s.length(),false);
    }
}
