package com.doomcatlee.licensedecoder.handlers;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;

public class BarcodeParser {

    protected HashMap<String, String> headers;
    protected HashMap<String, String> originalData;
    protected HashMap<String, String> data;
    static final HashMap<String, String> fields; // Mapping fields from AAMVA standards
    static {
        fields = new HashMap<>();

        fields.put("DAA", "Name");
        fields.put("DLDAA", "Name");
        fields.put("DAB", "LastName");
        fields.put("DCS", "LastName");
        fields.put("DAC", "FirstName");
        fields.put("DCT", "FirstName");
        fields.put("DAD", "MiddleName");

        fields.put("DBC", "Sex");
        fields.put("DAU", "Height");
        fields.put("DAY", "EyeColor");

        fields.put("DAG", "Address");
        fields.put("DAI", "City");
        fields.put("DAN", "City");
        fields.put("DAJ", "State");
        fields.put("DAO", "State");
        fields.put("DAK", "ZipCode");
        fields.put("DAP", "Zipcode");
        fields.put("DCG", "Country");

        fields.put("DBB", "DOB");
        fields.put("DAQ", "DriverLicenseNumber");
        fields.put("DBD", "LicenseIssuedDate");
        fields.put("DBA", "LicenseExpirationDate");
    }

    // Upon init, parse all data and save as new object
    public BarcodeParser(String barcode) {
        headers = decodeHeaders(barcode);
        originalData = decodeContent(barcode);
        data = decodeData();
    }

    /**
     * Decode header portion of barcodedecoder String to HashMap object.
     *
     *          (ex)
     *              {
     *                  "SubfileOffset": "8",
     *                  "SubfileLength": "123"
     *              }
     *
     * **/
    protected HashMap<String, String> decodeHeaders(String barcode) {
        HashMap headerMap = new HashMap();

        // declare header variables
        char complianceIndicator, dataElementSeparator, recordSeparator, segmentTerminator;
        String fileType, entries, subfileType;
        int versionNumber, issuerIdentificationNumber, jurisdictionVersion, offset, length;

        // extract headers
        complianceIndicator = barcode.charAt(0);
        dataElementSeparator = barcode.charAt(1);
        recordSeparator = barcode.charAt(2);
        segmentTerminator = barcode.charAt(3);

        fileType = barcode.substring(4, 9);
        headerMap.put("FileType", fileType);
        issuerIdentificationNumber = Integer.parseInt(barcode.substring(9, 15));
        headerMap.put("IdentificationNumber", issuerIdentificationNumber);
        versionNumber = Integer.parseInt(barcode.substring(15, 17));
        headerMap.put("VersionNumber", versionNumber);

        if (versionNumber <= 1) {
            entries = barcode.substring(17, 19);
            subfileType = barcode.substring(19, 21);
            offset = Integer.parseInt(barcode.substring(21, 25));
            length = Integer.parseInt(barcode.substring(25, 29));
        } else {
            jurisdictionVersion = Integer.parseInt(barcode.substring(17, 19));
            headerMap.put("JurisdictionVerstion", jurisdictionVersion);
            entries = barcode.substring(19, 21);
            subfileType = barcode.substring(21, 23);
            headerMap.put("SubfileType", subfileType);
            offset = Integer.parseInt(barcode.substring(23, 27));
            length = Integer.parseInt(barcode.substring(27, 31));
        }

        if (fileType.equals("ANSI ")) {
            offset += 2;
        }

        headerMap.put("SubfileOffset", offset);
        headerMap.put("SubfileLength", length);

        return headerMap;
    }

    /**
     * Decode content of barcodedecoder String to HashMap object.
     *
     * First layer of parsing:
     *          1. Based on length and offset, loop through keys and values.
     *          2. Take the first three char of each line, which represents code (ex) "DAQ", "DBB"
     *          3. Return HashMap of code and value
     *
     *          (ex)
     *              {
     *                  "DAQ": "Dong Kun Lee",
     *                  "DBB": "08211993"
     *              }
     *
     * **/
    protected HashMap<String, String> decodeContent(String barcode) {
        int offset = getSubfileOffset();
        int length = getSubfileLength();

        // content
        String content = barcode.substring(offset, offset + length);

        // store in name value pair
        String[] lines = content.split("\n");
        HashMap<String, String> hashMap = new HashMap<>();
        for (String l : lines) {
            if (l.length() > 3) {
                String key = l.substring(0, 3);
                String value = l.substring(3);
                if (fields.get(key) != null) {
                    hashMap.put(fields.get(key), value);
                }
            }
        }
        return hashMap;
    }

    protected HashMap<String, String> decodeData() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        HashMap hashMap = new HashMap();

        hashMap.put("firstName", getFirstName());
        hashMap.put("middleName", getMiddleName());
        hashMap.put("lastName", getLastName());
        hashMap.put("address", getAddress());
        hashMap.put("city", getCity());
        hashMap.put("state", getState());
        hashMap.put("zipcode", getZipCode());
        hashMap.put("driverLicenseNumber", getDriverLicenseNumber());
        hashMap.put("eyeColor", getEyeColor());
        hashMap.put("height", getHeight());
        hashMap.put("sex", getSex());

        hashMap.put("dob", getDOB().format(formatter));
        hashMap.put("licenseIssuedDate", getLicenseIssuedDate().format(formatter));
        hashMap.put("licenseExpirationDate",
                getLicenseExpirationDate().format(formatter));

        return hashMap;
    }

    // -------------------------------------------------------------------------------- //

    /**
     * Get extracted first name or parse from name string.
     *
     * 1. Depending on state, it could be firstName, middleName, lastName or just name
     * 2. If first name, then return first name
     * 3. If just name, then need to parse the name to first middle and last name
     *
     * @return
     *
     *         Examples:
     *
     *         Name: "Dong Lee", firstName: "DongLee";
     *
     *         Name: "Dong Kun Lee", firstName: "Dong";
     *
     *         Name: "Dong Kun Babak Lee", firstName: "Dong Kun Babak";
     *
     *         Name: "Lee, Dong Kun", firstName: "Dong";
     *
     *
     */
    public String getFirstName() {
        String firstName = originalData.get("FirstName");
        // Grab firstName if it exists
        if (firstName != null && !firstName.isEmpty()) {
            firstName = firstName.trim();

        // Parse Name field
        } else {
            String name = originalData.get("Name");

            // Oregon
            if (name.contains(",")) {
                firstName = parseFirstNameWithComma();
                return firstName;
            }

            // Normal parsing
            if (name != null && !name.isEmpty()) {
                String[] nameTokens = name.split(" ");
                if (nameTokens.length <= 3) {
                    firstName = nameTokens[0].trim();
                } else {
                    for (int i = 1; i < nameTokens.length; i++) {
                        firstName += nameTokens[i].trim();
                        if (i < nameTokens.length - 1) {
                            firstName += " ";
                        }
                    }
                }
            } else {
                firstName = "";
            }
        }
        return firstName;
    }

    // For States that has commas in name
    /** Oregon Syntax:
     *
     * LastName, FirstName, MiddleName, Suffix
     *
     * Name: "Lee, Dong Kun", firstName: "Dong Kun"
     *
     * Name: "Lee, Dong Kun, Babak", firstName: "Dong Kun"
     *
     * Name: "Lee, Dong Kun, Babak, Jr", firstName: "Dong Kun"
     *
    **/
    public String parseFirstNameWithComma() {
        String firstName = "";
        String name = originalData.get("Name");

        if (name != null && !name.isEmpty()) {
            String[] splitNames = name.split(",");
            // Trim any white spaces
            for (int i = 0; i < splitNames.length; i++) {
                splitNames[i] = splitNames[i].trim();
            }

            // if only one name
            if (splitNames.length > 1) {
                firstName = splitNames[1];
            } else {
                firstName = splitNames[0];
            }
        }

        return firstName;
    }

    /**
     * Get extracted last name or parse from name string.
     *
     * @return
     *
     *         Examples:
     *
     *         Name: "Dong Lee", lastName: "Lee";
     *
     *         Name: "Dong Kun Lee", lastName: "Lee";
     *
     *         Name: "Dong Kun Babak Lee", lastName: "Lee";
     *
     */
    public String getLastName() {
        String lastName = originalData.get("LastName");
        if (lastName != null && !lastName.isEmpty()) {
            lastName = lastName.trim();
        } else {
            String name = originalData.get("Name");
            if (name.contains(",")) {
                lastName = parseLastNameWithComma();
                return lastName;
            }
            if (name != null && !name.isEmpty()) {
                String[] nameTokens = name.split(" ");
                if (nameTokens.length == 1) {
                    lastName = "";
                } else {
                    lastName = nameTokens[nameTokens.length - 1].trim();
                }
            } else {
                lastName = "";
            }
        }
        return lastName;
    }

    /** Oregon Syntax:
     *
     * LastName, FirstName, MiddleName, Suffix
     *
     * Name: "Lee, Dong Kun", lastName: "Lee"
     *
     * Name: "Lee, Dong Kun, Babak", lastName: "Lee"
     *
     * Name: "Lee, Dong Kun, Babak, Jr", lastName: "Lee"
     *
     **/
    String parseLastNameWithComma() {
        String lastName = "";
        String name = originalData.get("Name");

        if (name != null && !name.isEmpty()) {
            String[] splitNames = name.split(",");
            // Trim any white spaces
            for (int i = 0; i < splitNames.length; i++) {
                splitNames[i] = splitNames[i].trim();
            }
            lastName = splitNames[0]; // always the first item
        }

        return lastName;
    }

    /**
     * Get extracted middle name or parse from name string.
     *
     * @return
     *
     *         Examples:
     *
     *         Name: "DongKun Lee", middleName: "";
     *
     *         Name: "Dong Kun Lee", middleName: "Kun";
     *
     *         Name: "Dong Kun James Lee", middleName: "";
     *
     */
    public String getMiddleName() {
        String middleName = originalData.get("MiddleName");
        if (middleName != null && !middleName.isEmpty()) {
            middleName = middleName.trim();
        } else {
            String name = originalData.get("Name");

            if (name.contains(",")) {
                middleName = parseMiddleNameWithComma();
                return middleName;
            }

            if (name != null && !name.isEmpty()) {
                String[] nameTokens = name.split(" ");
                if (nameTokens.length == 3) {
                    middleName = nameTokens[1].trim();
                } else {
                    middleName = "";
                }
            } else {
                middleName = "";
            }
        }
        return middleName;
    }


    /** Oregon Syntax:
     *
     * LastName, FirstName, MiddleName, Suffix
     *
     * Name: "Lee, Dong", middleName: ""
     *
     * Name: "Lee, Dong Kun", middleName: ""
     *
     * Name: "Lee, Dong Kun, Babak", middleName: "Babak"
     *
     * Name: "Lee, Dong Kun, Babak, Jr", middleName: "Babak"
     *
     **/
    public String parseMiddleNameWithComma() {
        String middleName = "";
        String name = originalData.get("Name");

        if (name != null && !name.isEmpty()) {
            String[] splitNames = name.split(",");
            // Trim any white spaces
            for (int i = 0; i < splitNames.length; i++) {
                splitNames[i] = splitNames[i].trim();
            }

            // If and only if greater than 2, there's a middle name
            if (splitNames.length > 2) {
                middleName = splitNames[2]; // middle names always the third item
            }
        }

        return middleName;
    }

    /**
     * Get extracted state
     *
     * @return 2-Letter state abbreviations
     */
    public String getState() {
        String state = originalData.get("State");
        if (state != null && !state.isEmpty()) {
            state = state.trim().toUpperCase();
        } else {
            state = "";
        }
        return state;
    }

    /**
     * Get extracted address
     *
     * @return Address
     */
    public String getAddress() {
        String address = originalData.get("Address");
        if (address != null && !address.isEmpty()) {
            address = address.trim();
        } else {
            address = "";
        }
        return address;
    }

    /**
     * Get extracted city
     *
     * @return City
     */
    public String getCity() {
        String city = originalData.get("City");
        if (city != null && !city.isEmpty()) {
            city = city.trim();
        } else {
            city = "";
        }
        return city;
    }

    /**
     * Get extracted ZIP code
     *
     * @return ZIP code
     */
    public String getZipCode() {
        String zipCode = originalData.get("ZipCode");
        if (zipCode != null && !zipCode.isEmpty()) {
            zipCode = zipCode.trim();
        } else {
            zipCode = "";
        }
        return zipCode;
    }

    /**
     * Get extracted country
     *
     * @return Country
     */
    public String getCountry() {
        String country = originalData.get("Country");
        if (country != null && !country.isEmpty()) {
            country = country.trim().toUpperCase();
        } else {
            country = "";
        }
        return country;
    }

    /**
     * Get extracted eye color
     *
     * @return Eye color
     */
    public String getEyeColor() {
        String eyeColor = originalData.get("EyeColor");
        if (eyeColor != null && !eyeColor.isEmpty()) {
            eyeColor = eyeColor.trim();
        } else {
            eyeColor = "";
        }
        return eyeColor;
    }

    /**
     * Get extracted driver's license number
     *
     * @return Driver's license number
     */
    public String getDriverLicenseNumber() {
        String licenseNumber = originalData.get("DriverLicenseNumber");
        if (licenseNumber != null && !licenseNumber.isEmpty()) {
            licenseNumber = licenseNumber.trim().replaceAll("[.]", "");
        } else {
            licenseNumber = "";
        }
        return licenseNumber;
    }

    /**
     * Parse Sex variable
     * Depending on the state, sex comes out as 1 or 2. Otherwise just M or F.
     *
     * @return Sex
     */
    public String getSex() {
        String sex = originalData.get("Sex");
        if (sex != null && !sex.isEmpty()) {
            sex = sex.trim();
            if (sex.equals("1")) {
                sex = "M";
            } else if (sex.equals("2")) {
                sex = "F";
            } else {
                sex = sex.toUpperCase();
            }
        } else {
            sex = "";
        }
        return sex;
    }

    /**
     * Get parsed DOB
     *
     * @return DOB
     */
    public LocalDate getDOB() {
        LocalDate localDate = null;
        String dob = originalData.get("DOB");
        if (dob != null && !dob.isEmpty()) {
            localDate = parseDate(dob);
        } else {
            // Not found
        }
        return localDate;
    }

    /**
     * Get parsed LicenseIssuedDate
     *
     * @return LicenseIssuedDate
     */
    public LocalDate getLicenseIssuedDate() {
        LocalDate localDate = null;
        String licenseIssuedDate = originalData.get("LicenseIssuedDate");
        if (licenseIssuedDate != null && !licenseIssuedDate.isEmpty()) {
            localDate = parseDate(licenseIssuedDate);
        }

        return localDate;
    }

    /**
     * Get parsed LicenseExpirationDate
     *
     * @return LicenseExpirationDate
     */
    public LocalDate getLicenseExpirationDate() {
        LocalDate localDate = null;
        String licenseExpirationDate = originalData.get("LicenseExpirationDate");
        if (licenseExpirationDate != null && !licenseExpirationDate.isEmpty()) {
            localDate = parseDate(licenseExpirationDate);
        } else {
            // Not found
        }
        return localDate;
    }

    /**
     * Get parsed Height
     *
     * @return Height
     */
    public double getHeight() {
        String height = originalData.get("Height");
        if (height != null && !height.isEmpty()) {
            height = height.trim().replaceAll("[\\D]", ""); // remove any non-digits
        } else {
            height = "";
        }
        return Double.parseDouble(height);
    }

    protected LocalDate parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate parsedDate = LocalDate.parse(date, formatter);

        return parsedDate;
    }

    protected String formatDate(Calendar date) {
        String result = "";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        result = sdf.format(date.getTime());
        return result;
    }

    // -------------------------------------------------------------------------------- //
    /** Getters **/

    public HashMap getHeaders() {
        return headers;
    }

    public HashMap<String, String> getOriginalData() {
        return originalData;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public String getFileType() {
        String result = (String) getHeaders().get("FileType");
        if (result != null && !result.isEmpty()) {
            result = result.trim().toUpperCase();
        } else {
            result = "";
        }
        return result;
    }

    public int getIdentificationNumber() {
        return Integer.parseInt(getHeaders().get("IdentificationNumber").toString());
    }

    public int getVersionNumber() {
        return Integer.parseInt(getHeaders().get("VersionNumber").toString());
    }

    public int getJurisdictionVersion() {
        return Integer.parseInt(getHeaders().get("JurisdictionVersion").toString());
    }

    public String getSubfileType() {
        String result = (String) getHeaders().get("SubfileType");
        if (result != null && !result.isEmpty()) {
            result = result.trim();
        } else {
            result = "";
        }
        return result;
    }

    public int getSubfileOffset() {
        return Integer.parseInt(getHeaders().get("SubfileOffset").toString());
    }

    public int getSubfileLength() {
        return Integer.parseInt(getHeaders().get("SubfileLength").toString());
    }
}