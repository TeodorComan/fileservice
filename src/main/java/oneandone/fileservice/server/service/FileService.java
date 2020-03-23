package oneandone.fileservice.server.service;

import java.util.List;
import java.util.Optional;

import oneandone.fileservice.server.model.File;
import oneandone.fileservice.server.service.impl.FileReadOption;

public interface FileService {

    Optional<File> get(String name, FileReadOption... fileReadOption);

    void delete(String name);

    void create(File file);

    void update (String fileName, File file);

    long count();

    List<String> getFilename(String regex);
}
