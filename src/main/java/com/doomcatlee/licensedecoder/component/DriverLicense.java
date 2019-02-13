package com.doomcatlee.licensedecoder.component;

import com.doomcatlee.licensedecoder.handlers.BarcodeParser;

import java.util.HashMap;

public class DriverLicense {
    private String originalDataString;
    private HashMap<String, String> originalData;
    private HashMap<String, String> data;
    private BarcodeParser parser;

    // Initialize driver license object with original data and parsed data
    public DriverLicense(String barCode) {
        originalDataString = barCode;
        parser = new BarcodeParser(barCode);
        originalData = parser.getOriginalData();
        data = parser.getData();
    }

    public String getOriginalDataString() {
        return originalDataString;
    }

    public HashMap<String, String> getOriginalData() {
        return originalData;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public BarcodeParser getParser() {
        return parser;
    }
}


