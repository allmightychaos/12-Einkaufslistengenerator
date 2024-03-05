import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class Einkaufslistengenerator extends JFrame {
    private JComboBox<String> cbProduktgruppe, cbProdukte;
    private JTextField tfEigeneProdukte;
    private JSpinner spAnzahl;
    private JTable table;
    private DefaultTableModel tableModel;
    private final HashMap<String, List<String>> produktgruppen;

    public Einkaufslistengenerator() {
        produktgruppen = new HashMap<>();
        loadProducts();
        initUI();
    }

    private void loadProducts() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Produkte.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                produktgruppen.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        setTitle("Einkaufslistengenerator");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        JPanel panelOben = new JPanel();
        cbProduktgruppe = new JComboBox<>(produktgruppen.keySet().toArray(new String[0]));
        cbProduktgruppe.addActionListener(this::changeProducts);
        cbProdukte = new JComboBox<>();
        tfEigeneProdukte = new JTextField(10);
        spAnzahl = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));

        panelOben.add(cbProduktgruppe);
        panelOben.add(cbProdukte);
        panelOben.add(tfEigeneProdukte);
        panelOben.add(spAnzahl);

        JButton btnHinzufuegen = new JButton("Hinzufügen");
        btnHinzufuegen.addActionListener(e -> addProducts());
        panelOben.add(btnHinzufuegen);

        getContentPane().add(panelOben, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Produktgruppe", "Produkt", "Anzahl"}, 0);
        table = new JTable(tableModel);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnLoeschen = new JButton("Löschen");
        btnLoeschen.addActionListener(e -> deleteEntries());
        getContentPane().add(btnLoeschen, BorderLayout.SOUTH);

        JMenuBar menuBar = getjMenuBar();
        setJMenuBar(menuBar);

        changeProducts(null);
        addEnterKeyListener();
        addDeleteShortcut();

        cbProdukte.addActionListener(e -> customProduct());
    }

    private JMenuBar getjMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuDatei = new JMenu("Datei");
        JMenuItem itemNeu = new JMenuItem("Neu");
        itemNeu.addActionListener(e -> newList());
        JMenuItem itemSpeichern = new JMenuItem("Speichern");
        itemSpeichern.addActionListener(e -> saveFile());
        JMenuItem itemLaden = new JMenuItem("Laden");
        itemLaden.addActionListener(e -> loadList());

        menuDatei.add(itemNeu);
        menuDatei.add(itemSpeichern);
        menuDatei.add(itemLaden);
        menuBar.add(menuDatei);
        return menuBar;
    }

    private void changeProducts(ActionEvent e) {
        String produktgruppe = (String) cbProduktgruppe.getSelectedItem();
        List<String> produkte = new ArrayList<>(produktgruppen.get(produktgruppe));
        produkte.add("Weitere");
        cbProdukte.setModel(new DefaultComboBoxModel<>(produkte.toArray(new String[0])));
        cbProdukte.setSelectedIndex(0);
        tfEigeneProdukte.setEnabled(false);
    }

    private void customProduct() {
        tfEigeneProdukte.setEnabled(cbProdukte.getSelectedItem().equals("Weitere"));
        if (cbProdukte.getSelectedItem().equals("Weitere")) {
            tfEigeneProdukte.requestFocus();
        } else {
            tfEigeneProdukte.setText("");
        }
    }

    private void addProducts() {
        String produktgruppe = (String) cbProduktgruppe.getSelectedItem();
        String produkt = tfEigeneProdukte.getText().isEmpty() ? (String) cbProdukte.getSelectedItem() : tfEigeneProdukte.getText();
        int anzahl = (Integer) spAnzahl.getValue();
        tableModel.addRow(new Object[]{produktgruppe, produkt, anzahl});
    }

    private void deleteEntries() {
        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    private void newList() {
        tableModel.setRowCount(0);
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getPath().toLowerCase().endsWith(".xml")) {
                file = new File(file.getPath() + ".xml");
            }
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                Element rootElement = doc.createElement("Einkaufsliste");
                doc.appendChild(rootElement);

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Element produkt = doc.createElement("Produkt");
                    produkt.setAttribute("Produktgruppe", tableModel.getValueAt(i, 0).toString());
                    produkt.setAttribute("Name", tableModel.getValueAt(i, 1).toString());
                    produkt.setAttribute("Anzahl", tableModel.getValueAt(i, 2).toString());
                    rootElement.appendChild(produkt);
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(file);
                transformer.transform(source, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadList() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                tableModel.setRowCount(0);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                NodeList nList = doc.getElementsByTagName("Produkt");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        String produktgruppe = eElement.getAttribute("Produktgruppe");
                        String name = eElement.getAttribute("Name");
                        int anzahl = Integer.parseInt(eElement.getAttribute("Anzahl"));
                        tableModel.addRow(new Object[]{produktgruppe, name, anzahl});
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addEnterKeyListener() {
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addProducts();
                }
            }
        };

        tfEigeneProdukte.addKeyListener(enterKeyAdapter);
        cbProdukte.addKeyListener(enterKeyAdapter);
        cbProduktgruppe.addKeyListener(enterKeyAdapter);
        ((JSpinner.DefaultEditor) spAnzahl.getEditor()).getTextField().addKeyListener(enterKeyAdapter);
    }

    private void addDeleteShortcut() {
        KeyStroke deleteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

        Action deleteAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteEntries();
            }
        };

        table.getInputMap(JComponent.WHEN_FOCUSED).put(deleteKeyStroke, "deleteEntry");
        table.getActionMap().put("deleteEntry", deleteAction);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Einkaufslistengenerator().setVisible(true));
    }
}
