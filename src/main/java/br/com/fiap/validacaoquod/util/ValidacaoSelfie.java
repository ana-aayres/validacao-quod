package br.com.fiap.validacaoquod.util;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class ValidacaoSelfie {

    public static Map<String, String> extrairMetadados(MultipartFile imagem) {
        Map<String, String> metadados = new HashMap<>();
        try (InputStream input = imagem.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(input);

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String tagValue = tag.getDescription();

                    // Apenas capturamos informações relevantes
                    if (tagName.equals("Date/Time Original") || tagName.startsWith("GPS")) {
                        metadados.put(tagName, tagValue);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadados;
    }


    public static boolean validarMesmoDia(Map<String, String> selfie1, Map<String, String> selfie2) {
        String dataSelfie1 = selfie1.get("Date/Time Original");
        String dataSelfie2 = selfie2.get("Date/Time Original");
    
        boolean mesmaData = false;
    
        try {
            if (dataSelfie1 != null && !dataSelfie1.equals("Não encontrado") &&
                dataSelfie2 != null && !dataSelfie2.equals("Não encontrado")) {
    
                SimpleDateFormat formato = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                Date date1 = formato.parse(dataSelfie1);
                Date date2 = formato.parse(dataSelfie2);
    
                SimpleDateFormat apenasDia = new SimpleDateFormat("yyyy-MM-dd");
                mesmaData = apenasDia.format(date1).equals(apenasDia.format(date2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return mesmaData;
    }

    public static boolean validarMesmaLocalizacao(Map<String, String> selfie1, Map<String, String> selfie2) {
        String gpsLat1 = selfie1.get("GPS Latitude");
        String gpsLon1 = selfie1.get("GPS Longitude");
        String gpsLat2 = selfie2.get("GPS Latitude");
        String gpsLon2 = selfie2.get("GPS Longitude");

        return gpsLat1 != null && gpsLat1.equals(gpsLat2) && gpsLon1 != null && gpsLon1.equals(gpsLon2);
    }

    public static Map<String, String> preencherMetadados(Map<String, String> metadados) {
        List<String> chaves = List.of(
            "GPS Longitude", "GPS Latitude", "GPS Latitude Ref",
            "Date/Time Original", "GPS Longitude Ref"
        );

        for (String chave : chaves) {
            metadados.put(chave, metadados.getOrDefault(chave, "Não encontrado"));
        }

        return metadados;
    }
}
