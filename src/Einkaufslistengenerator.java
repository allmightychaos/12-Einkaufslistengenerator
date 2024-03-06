import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class Einkaufslistengenerator extends JFrame {
    private JComboBox<String> cbProductsgroup, cbProducts;
    private JTextField tfCustomProducts;
    private JSpinner spQuantity;
    private JTable table;
    private DefaultTableModel tableModel;
    private Map<String, List<String>> productgroups;

    public Einkaufslistengenerator() {
        productgroups = new TreeMap<>();
        loadProducts();
        autoSorter();
        initUI();
    }

    private void loadProducts() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Produkte.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                productgroups.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
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
        cbProductsgroup = new JComboBox<>(productgroups.keySet().toArray(new String[0]));
        cbProductsgroup.addActionListener(this::changeProducts);
        cbProducts = new JComboBox<>();
        tfCustomProducts = new JTextField(10);
        spQuantity = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));

        panelOben.add(cbProductsgroup);
        panelOben.add(cbProducts);
        panelOben.add(tfCustomProducts);
        panelOben.add(spQuantity);

        JButton btnHinzufuegen = new JButton("Hinzufügen");
        btnHinzufuegen.addActionListener(e -> addProducts());
        panelOben.add(btnHinzufuegen);

        getContentPane().add(panelOben, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Produktgruppe", "Produkt", "Anzahl"}, 0);
        table = new JTable(tableModel);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnLoeschen = new JButton("Löschen");
        JButton btnSortieren = new JButton("Sortieren");

        btnLoeschen.addActionListener(e -> deleteEntries());
        btnSortieren.addActionListener(e -> sortEntries());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnLoeschen);
        buttonPanel.add(btnSortieren);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Menubar
        JMenuBar menuBar = new JMenuBar();
        
        // Menü "Datei"
        JMenu menuDatei = new JMenu("Datei");
        
        // Item-1 "Neu"
        JMenuItem itemNeu = new JMenuItem("Neu");
        itemNeu.addActionListener(e -> newList());

        // Item-2 "Speichern"
        JMenuItem itemSpeichern = new JMenuItem("Speichern");
        itemSpeichern.addActionListener(e -> saveFile());

        // Item-3 "Laden"
        JMenuItem itemLaden = new JMenuItem("Laden");
        itemLaden.addActionListener(e -> loadList());

        // Item-4 "Drucken"
        JMenuItem itemDrucken = new JMenuItem("Drucken");
        itemDrucken.addActionListener(e -> printItems());

        menuDatei.setMnemonic(KeyEvent.VK_D); // Mnemonic 'D' für "Datei"

        itemNeu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemNeu.setMnemonic(KeyEvent.VK_N); // Mnemonic 'N'

        itemLaden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemLaden.setMnemonic(KeyEvent.VK_L); // Mnemonic 'L'

        itemSpeichern.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemSpeichern.setMnemonic(KeyEvent.VK_S); // Mnemonic 'S'

        itemDrucken.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemDrucken.setMnemonic(KeyEvent.VK_P); // Mnemonic 'P'

        menuDatei.add(itemNeu);
        menuDatei.add(itemLaden);
        menuDatei.add(itemSpeichern);
        menuDatei.add(itemDrucken);
        menuBar.add(menuDatei);
        setJMenuBar(menuBar);

        changeProducts(null);
        addEnterKeyListener();
        addDeleteShortcut();

        cbProducts.addActionListener(e -> customProduct());
    }

    private void sortEntries() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING)); // Produktgruppe   (Spalte 0)
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING)); // Produkte        (Spalte 1)

        sorter.setSortKeys(sortKeys);
        sorter.sort();
    }

    private void autoSorter() {
        Map<String, List<String>> sortedproductgroups = new TreeMap<>();

        productgroups.forEach((gruppe, produkte) -> {
            Collections.sort(produkte);
            sortedproductgroups.put(gruppe, produkte);
        });

        productgroups = sortedproductgroups;
    }

    private void changeProducts(ActionEvent e) {
        String produktgruppe = (String) cbProductsgroup.getSelectedItem();
        List<String> produkte = new ArrayList<>(productgroups.get(produktgruppe));
        produkte.add("Weitere");
        cbProducts.setModel(new DefaultComboBoxModel<>(produkte.toArray(new String[0])));
        cbProducts.setSelectedIndex(0);
        tfCustomProducts.setEnabled(false);
    }

    private void customProduct() {
        tfCustomProducts.setEnabled(cbProducts.getSelectedItem().equals("Weitere"));
        if (cbProducts.getSelectedItem().equals("Weitere")) {
            tfCustomProducts.requestFocus();
        } else {
            tfCustomProducts.setText("");
        }
    }

    private void addProducts() {
        String produktgruppe = (String) cbProductsgroup.getSelectedItem();
        String produkt = tfCustomProducts.getText().isEmpty() ? (String) cbProducts.getSelectedItem() : tfCustomProducts.getText();
        int anzahl = (Integer) spQuantity.getValue();
        boolean exists = false;

        // Durchlaufe die Tabelle und suche nach dem Produkt
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String existingProductsgroup = (String) tableModel.getValueAt(i, 0);
            String existingProducts = (String) tableModel.getValueAt(i, 1);
            int existingQuantity = (Integer) tableModel.getValueAt(i, 2);

            // Wenn das Produkt und die Produktgruppe übereinstimmen, aktualisiere die Anzahl
            if (existingProductsgroup.equals(produktgruppe) && existingProducts.equals(produkt)) {
                int newQuantity = existingQuantity + anzahl;
                tableModel.setValueAt(newQuantity, i, 2);
                exists = true;
                break;
            }
        }

        // Wenn das Produkt nicht existiert, füge es als neuen Eintrag hinzu
        if (!exists) {
            tableModel.addRow(new Object[]{produktgruppe, produkt, anzahl});
        }
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

    private void printItems() {
        try {
            table.print();
        } catch (Exception e) {
            e.printStackTrace();
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

        tfCustomProducts.addKeyListener(enterKeyAdapter);
        cbProducts.addKeyListener(enterKeyAdapter);
        cbProductsgroup.addKeyListener(enterKeyAdapter);
        ((JSpinner.DefaultEditor) spQuantity.getEditor()).getTextField().addKeyListener(enterKeyAdapter);
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
