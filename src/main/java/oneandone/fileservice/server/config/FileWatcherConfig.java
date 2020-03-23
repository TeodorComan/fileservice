package oneandone.fileservice.server.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileWatcherConfig {

    @Autowired
    private FileChangeListener fileChangeListener;

    @Value("${rootFolderPath}")
    private String rootFolderPath;

    @Value("${folderPoolInterval}")
    private long folderPoolInterval;

    @Value("${folderPoolQuietPeriod}")
    private long folderPoolQuietPeriod;

    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherConfig.class);

    @Bean
    public FileSystemWatcher fileSystemWatcher() {

        Path rootFolder = Paths.get(this.rootFolderPath).toAbsolutePath().normalize();

        FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(true, Duration.ofMillis(folderPoolInterval), Duration.ofMillis(folderPoolQuietPeriod));
        fileSystemWatcher.addSourceFolder(rootFolder.toFile());
        fileSystemWatcher.addListener(fileChangeListener);
        fileSystemWatcher.start();
        LOG.info("Started watching folder: {}",rootFolder);
        return fileSystemWatcher;
    }

    @PreDestroy
    public void onDestroy() {
        fileSystemWatcher().stop();
    }
}