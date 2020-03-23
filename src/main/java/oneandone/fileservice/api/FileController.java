package oneandone.fileservice.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import oneandone.fileservice.server.model.File;
import oneandone.fileservice.server.service.FileService;
import oneandone.fileservice.server.service.impl.FileReadOption;

@RestController
@RequestMapping("/file-service/v1/files")
public class FileController {

    @Autowired(required = false)
    private FileService fileService;

    @RequestMapping(path="/{name}/download", method = RequestMethod.GET)
    public ResponseEntity downloadFile(@PathVariable("name") String name) {

        Optional<File> optionalFile = fileService.get(name);

        if(!optionalFile.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        File file = optionalFile.get();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(file.getContent());
    }

    @RequestMapping(path="/{name}", method = RequestMethod.GET)
    public ResponseEntity<File> getFile(@PathVariable("name") String name) {

        Optional<File> optionalFile = fileService.get(name);

        if(optionalFile.isPresent()){
            return ResponseEntity.ok(fileService.get(name).get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = RequestMethod.HEAD,path = "/{name}")
    public ResponseEntity<Void> getFileHeaders(@PathVariable("name") String fileName, HttpServletResponse response) {
        Optional<File> file = fileService.get(fileName, FileReadOption.METADATA);
        ResponseEntity<Void> responseEntity;

        if(file.isPresent()) {
            response.addHeader("X-Last-Modified", String.valueOf(file.get().getLastModified()));
            responseEntity = ResponseEntity.ok().build();
        } else {
            responseEntity = ResponseEntity.notFound().build();
        }

        return responseEntity;
    }

    @PostMapping(path = "/{name}")
    public ResponseEntity<Void> update(@PathVariable("name") String fileName, @RequestBody File file) {
        fileService.update(fileName, file);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    public ResponseEntity<List<String>> getFiles(@RequestParam String regex, HttpServletResponse response) {
        List<String> files = fileService.getFilename(regex);
        if(files.isEmpty()){
            response.addHeader("X-Total-Count","0");
            return ResponseEntity.notFound().build();
        } else {
            response.addHeader("X-Total-Count",String.valueOf(files.size()));
            return ResponseEntity.ok(files);
        }

    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> countFiles(HttpServletResponse response) {
        response.addHeader("X-Total-Count",String.valueOf(fileService.count()));
        return ResponseEntity.ok().build();
    }

    @PutMapping(path="/upload")
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile uploadedFile) throws IOException {

        File file = new File();
        file.setName(uploadedFile.getOriginalFilename());
        file.setContent(uploadedFile.getBytes());

        fileService.create(file);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping
    public ResponseEntity<Void> create(@RequestBody File file) {

        fileService.create(file);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(path = "/{name}")
    public ResponseEntity<Void> delete(String name) {
        fileService.delete(name);
        return ResponseEntity.accepted().build();
    }
}
