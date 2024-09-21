package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Test {

    private static final int NUM_PEOPLE = 1000; // Adjust to get ~10MB JSON
    private static final String JSON_FILE = "people.json";

    public static void main(String[] args) {
        try {
            // Generate JSON data (create the file)
            generateJsonFile(JSON_FILE, NUM_PEOPLE);

            // Benchmark using InputStream
            try (InputStream jsonInputStream = new FileInputStream(JSON_FILE)) {
                long jacksonTime = benchmarkJackson(jsonInputStream);
                System.out.println("Jackson parsing time: " + jacksonTime + " ms");
            }

            try (InputStream jsonInputStream = new FileInputStream(JSON_FILE)) {
                long jaxbTime = benchmarkJaxb(jsonInputStream);
                System.out.println("JAXB parsing time: " + jaxbTime + " ms");
            }

        } catch (IOException | JAXBException e) {
            e.printStackTrace();
        }

    }

    private static void generateJsonFile(String fileName, int numPeople) throws IOException {
        List<Person> people = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numPeople; i++) {
            String name = "Person" + i;
            int age = random.nextInt(100);
            people.add(new Person(name, age));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), people);
        System.out.println("Generated JSON file: " + fileName);
    }

    private static long benchmarkJackson(InputStream inputStream) throws IOException {
        long startTime = System.currentTimeMillis();

        ObjectMapper mapper = new ObjectMapper();
        Person[] people = mapper.readValue(inputStream, Person[].class);

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private static long benchmarkJaxb(InputStream inputStream) throws JAXBException, IOException {
        long startTime = System.currentTimeMillis();

        JAXBContext jaxbContext = JAXBContext.newInstance(PersonWrapper.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        StringBuilder jsonStringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
        }

        String xmlData = convertJsonToXml(jsonStringBuilder.toString());

        // Unmarshal the XML data into PersonWrapper
        PersonWrapper peopleWrapper = (PersonWrapper) unmarshaller.unmarshal(new StringReader(xmlData));

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private static String convertJsonToXml(String json) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<people>");

        String[] jsonEntries = json.substring(1, json.length() - 1).split("},");

        for (String entry : jsonEntries) {
            String formattedEntry = entry.replace("{", "").replace("}", "").trim();
            String[] fields = formattedEntry.split(",");

            xmlBuilder.append("<person>");
            for (String field : fields) {
                String[] keyValue = field.split(":");
                String key = keyValue[0].replace("\"", "").trim();
                String value = keyValue[1].replace("\"", "").trim();

                xmlBuilder.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
            }
            xmlBuilder.append("</person>");
        }

        xmlBuilder.append("</people>");

        return xmlBuilder.toString();
    }

}