package com.example.cicsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CicsController {
    private static final Logger logger = LoggerFactory.getLogger(CicsController.class);

    @Autowired
    private CicsService cicsService;

    @GetMapping("/")
    public String showForm() {
        return "cicsForm";
    }

    @PostMapping("/read")
    public String read(@RequestParam("key") String key, Model model) {
        try {
            String result = cicsService.performOperation("1", String.format("%-10s", key), "", "");
            model.addAttribute("result", result);
        } catch (Exception e) {
            logger.error("READ error: {}", e.getMessage());
            model.addAttribute("result", "Error: " + e.getMessage());
        }
        return "cicsForm";
    }

    @PostMapping("/write")
    public String write(@RequestParam("key") String key, 
                        @RequestParam("pass") String pass, 
                        @RequestParam("role") String role, 
                        Model model) {
        try {
            String result = cicsService.performOperation("2", String.format("%-10s", key), 
                                                        String.format("%-10s", pass), 
                                                        String.format("%-10s", role));
            model.addAttribute("result", result);
        } catch (Exception e) {
            logger.error("WRITE error: {}", e.getMessage());
            model.addAttribute("result", "Error: " + e.getMessage());
        }
        return "cicsForm";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("key") String key, Model model) {
        try {
            String result = cicsService.performOperation("3", String.format("%-10s", key), "", "");
            model.addAttribute("result", result);
        } catch (Exception e) {
            logger.error("DELETE error: {}", e.getMessage());
            model.addAttribute("result", "Error: " + e.getMessage());
        }
        return "cicsForm";
    }

    @PostMapping("/browse")
    public String browse(Model model) {
        logger.info("Handling BROWSE request");
        try {
            java.util.List<String> records = cicsService.browseAllRecords();
            String result = records.isEmpty() ? "No records found" : "Records: " + String.join(" | ", records);
            logger.info("BROWSE result: {}", result);
            model.addAttribute("result", result);
        } catch (Exception e) {
            logger.error("BROWSE error: {}", e.getMessage());
            model.addAttribute("result", "Error: " + e.getMessage());
        }
        return "cicsForm";
    }
}