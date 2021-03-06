package main;

import main.storage.StorageFileNotFoundException;
import main.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Created by Dean on 16/02/2017.
 */

@Controller
public class FileApi {

    private final StorageService storageService;
    private TempStorage temp = new TempStorage();

    @Autowired
    public FileApi(StorageService storageService) {
        this.storageService = storageService;
    }

    /***
     * Attaches new Document object to form on upload page
     * @param model
     * @return String
     */
    @GetMapping("/upload")
    public String uploadForm(Model model) {
        System.out.println("Upload get method firing and printing...");
        model.addAttribute("document", new Document());
        return "upload";
    }

    /***
     * Website to file system mapping
     * @param file
     * @param redirectAttributes
     * @return String
     */
    @PostMapping("/download")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes,
                                   @ModelAttribute Document document) {
        storageService.store(file);
        /** Test printing **/
        temp.addDoc(document);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");
        System.out.println("docCount: " + temp.getDocs().size());
        temp.printAll();

        return "redirect:/download";
    }

    /***
     * File system to website method
     * @param filename
     * @return ResponseEntity
     */
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFilename()+"\"")
                .body(file);
    }

    /***
     * Shows all uploaded files using Java 8 lambda mapping
     * @param model
     * @return String
     * @throws IOException
     */
    @GetMapping("/download")
    public String listUploadedFiles(Model model, @ModelAttribute Document document) throws IOException {
        System.out.println("Attempting to show files...");
        model.addAttribute("files", storageService
                .loadAll()
                .map(path ->
                        MvcUriComponentsBuilder
                                .fromMethodName(FileApi.class, "serveFile", path.getFileName().toString())
                                .build().toString())
                .collect(Collectors.toList()));
        model.addAttribute("documents", temp.getDocs());
        model.addAttribute("document", document);

        return "download";
    }






    /***
     * Handles FileNotFound exceptions
     * @param exc
     * @return ResponseEntity
     */
    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }




}
