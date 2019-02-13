package com.doomcatlee.licensedecoder.controller;

import com.doomcatlee.licensedecoder.component.DriverLicenseComponent;
import com.doomcatlee.licensedecoder.config.FileStorageService;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DriverLicenseController {
    @Autowired
    FileStorageService fileStorageService;
    @Autowired
    DriverLicenseComponent driverLicenseComponent;

    @RequestMapping(value = "/decodeDriverLicenseBarcode", method = RequestMethod.POST)
    @ApiOperation(value = "", response = String.class)
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileStorageService.storeFile(file); // store file to current location
            String fileName = file.getOriginalFilename();
            return driverLicenseComponent.decodeDriverLicense(fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new JSONObject().put("result", ex.getMessage()).toString();
        }
    }
}
