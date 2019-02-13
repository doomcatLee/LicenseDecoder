package com.doomcatlee.licensedecoder.component;

import com.doomcatlee.licensedecoder.handlers.BufferedImageLuminanceSource;
import com.google.gson.Gson;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class DriverLicenseComponent {
    private Gson gson = new Gson();

    /**
     * Given filePath of the barcode image, decode it then instantiate DriverLicense object.
     * **/
    public String decodeDriverLicense(String filePathName) {
        try {
            InputStream barCodeInputStream = new FileInputStream("uploads/" + filePathName);
            BufferedImage barCodeBufferedImage = ImageIO.read(barCodeInputStream);

            LuminanceSource source = new BufferedImageLuminanceSource(barCodeBufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap);
            String resultText = result.getText();

            // Create new driver license object
            DriverLicense license = new DriverLicense(resultText);
            return new JSONObject(gson.toJson(license.getData())).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return new JSONObject().put("result", "Failed").toString();
        }
    }
}
