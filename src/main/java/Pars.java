import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pars {
    private static final Logger LOGGER = Logger.getLogger(Pars.class.getName());
    private static final File[] selectedXsdFile = new File[1]; //наш xsd файл
    private static final int MAX_FILES_TO_PROCESS = 10;
    private static final long MAX_TOTAL_FILE_SIZE_BYTE = 512000;

    private static ArrayList<File> arrayFileToMerge = new ArrayList<>();

    private static String endPath = "";

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Выбор папок");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);

            // Создаем панель
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(4, 2));

            // Поле ввода для начальной папки
            JLabel label1 = new JLabel("Начальная папка:");
            JTextField startFolderField = new JTextField();
            panel.add(label1);
            panel.add(startFolderField);

            // Кнопка выбора начальной папки
            JButton startFolderButton = new JButton("Выбрать");
            startFolderButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser(new File("c:\\"));
                    chooser.setDialogTitle("Выберите начальную папку");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        File selectedFolder = chooser.getSelectedFile();
                        startFolderField.setText(selectedFolder.getAbsolutePath());
                    }
                }
            });
            panel.add(startFolderButton);

            // Поле ввода для конечной папки
            JLabel label2 = new JLabel("Конечная папка:");
            JTextField endFolderField = new JTextField();
            panel.add(label2);
            panel.add(endFolderField);

            // Кнопка выбора конечной папки
            JButton endFolderButton = new JButton("Выбрать");
            endFolderButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser(new File("c:\\"));
                    chooser.setDialogTitle("Выберите конечную папку");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        File selectedFolder = chooser.getSelectedFile();
                        endFolderField.setText(selectedFolder.getAbsolutePath());
                    }
                }
            });
            panel.add(endFolderButton);

            JLabel label3 = new JLabel("Файл:");
            JTextField fileField = new JTextField();
            panel.add(label3);
            panel.add(fileField);


            // Кнопка для выбора файла
            JButton fileButton = new JButton("Выбрать XSD файл");
            fileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser(new File("c:\\"));
                    fileChooser.setDialogTitle("Выберите файл");
                    if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        selectedXsdFile[0] = fileChooser.getSelectedFile();
                        fileField.setText(selectedXsdFile[0].getAbsolutePath());
                    }
                }
            });
            panel.add(fileButton);

            // Кнопки "Ок" и "Отмена"
            JButton okButton = new JButton("Ок");
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Здесь можно обработать выбранные папки
                    String startFolder = startFolderField.getText();
                    String endFolder = endFolderField.getText();
                    endPath = endFolder + "\\total.xml";
                    System.out.println("Начальная папка: " + startFolder);
                    System.out.println("Конечная папка: " + endFolder);
                    //ищем xml файлы
                    try {
                        findFile(startFolderField.getText());
                        mergeAndCreateTotalXml(arrayFileToMerge);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    frame.dispose(); // Закрываем окно
                }
            });
            panel.add(okButton);

            JButton cancelButton = new JButton("Отмена");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose(); // Закрываем окно
                }
            });
            panel.add(cancelButton);

            frame.add(panel);
            frame.setVisible(true);
        });
    }

    public static void findFile(String directoryPathtoFind) throws Exception {

        String extension = ".xml";

        File dir = new File(directoryPathtoFind);
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        if (files.length > MAX_FILES_TO_PROCESS) {
            LOGGER.log(Level.WARNING, "Превышено допустимое количество файлов для обьединения. Разрешенное значение 10 файлов. К обработке найдено " + files.length + " файлов");
            return;
        }
        if (files != null) {
            for (File file : files) {
                System.out.println("Найден XML-файл: " + file);
                if (validate(selectedXsdFile[0], file)) arrayFileToMerge.add(file);
            }
        }
    }

    public static boolean validate(File xsdFile, File xmlFile) throws Exception {
        boolean resalt = true;

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(xsdFile);

        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(xmlFile));
            LOGGER.log(Level.INFO, "Файл: " + xmlFile.getName() + " успешно прошел валидацию.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Файл: " + xmlFile.getName() + " НЕ ПРОШЕЛ ВАЛИДАЦИЮ.");
            resalt = false;
        }
        return resalt;
    }

    private static void mergeAndCreateTotalXml(List<File> files) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document totalDoc = docBuilder.newDocument();
            Element rootElement = totalDoc.createElement("Document");
            rootElement.setAttribute("id", UUID.randomUUID().toString());
            totalDoc.appendChild(rootElement);
            Element headersElement = totalDoc.createElement("Headers");
            rootElement.appendChild(headersElement);
            long totalFileSize = 0;
            for (File file : files) {
                Document fileDoc = docBuilder.parse(file);
                Element fileRoot = fileDoc.getDocumentElement();

                appendHeadersData(headersElement, fileRoot);
                Node importedNode = totalDoc.importNode(fileRoot, true);
                rootElement.appendChild(importedNode);
                totalFileSize += file.length();
            }
            if (totalFileSize > MAX_TOTAL_FILE_SIZE_BYTE) {
                LOGGER.log(Level.WARNING, "Файл Total.xml превышает 500 КБ.");
                return;
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(totalDoc);
            StreamResult result = new StreamResult(new File(endPath));
            transformer.transform(source, result);
        } catch (ParserConfigurationException | IOException | org.xml.sax.SAXException | TransformerException e) {
            e.printStackTrace();
            // бросить исключение
        }
    }

    private static void appendHeadersData(Element headersElement, Element fileRoot) {
        Element clientElement = (Element) fileRoot.getElementsByTagName("CUSTID").item(0);
        Element officeElement = (Element) fileRoot.getElementsByTagName("PAYER").item(0);
        Element branchElement = (Element) fileRoot.getElementsByTagName("PAYERBANKNAME").item(0);
        appendElement(headersElement, clientElement);
        appendElement(headersElement, officeElement);
        appendElement(headersElement, branchElement);
    }

    private static void appendElement(Element parent, Element element) {
        if (element != null) {
            Element clonedElement = (Element) parent.getOwnerDocument().importNode(element, true);
            parent.appendChild(clonedElement);
        }
    }
}


