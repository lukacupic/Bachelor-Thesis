package hr.fer.zemris.zavrsni;

import hr.fer.zemris.zavrsni.gui.GraphViewer;
import hr.fer.zemris.zavrsni.input.InputProcessor;
import hr.fer.zemris.zavrsni.input.PDFReader;
import hr.fer.zemris.zavrsni.input.QueryReader;
import hr.fer.zemris.zavrsni.model.Document;
import hr.fer.zemris.zavrsni.model.Result;
import hr.fer.zemris.zavrsni.ranking.RankingFunction;
import hr.fer.zemris.zavrsni.utils.GUIUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Luka Cupic
 */
public class GUI extends JFrame {

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    private static RankingFunction function;

    public GUI() {
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        //setUndecorated(true);
        setTitle("PDF Document Management and Search System");

        try {
            initGUI();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // GUI initialization

    private void initGUI() throws IOException {
        chooseDataset();

        JTabbedPane tabbedPane = createTabbedPane();
        add(tabbedPane);

        pack();
    }

    private static void chooseDataset() throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Choose Dataset Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.exit(1);
        }
        function = Initializer.init(chooser.getSelectedFile().toPath());
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel panel1 = createTab1();
        tabbedPane.addTab("Graph", panel1);

        JPanel panel2 = createTab2();
        tabbedPane.addTab("Query Search", panel2);

        JPanel panel3 = createTab3();
        tabbedPane.addTab("Document Search", panel3);

        return tabbedPane;
    }

    private JPanel createTab1() {
        JPanel panel = new JPanel();
        panel.add(GraphViewer.createViewer(WIDTH, HEIGHT));
        return panel;
    }

    private JPanel createTab2() {
        JPanel panel = new JPanel(new BorderLayout());

        JTable table = createTable();
        JPanel form = createPanel2Form(table);
        JPanel visualize = createVisualizePanel();

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(visualize, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTab3() {
        JPanel panel = new JPanel(new BorderLayout());

        JTable table = createTable();
        JPanel form = createPanel3Form(table);
        JPanel visualize = createVisualizePanel();

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(visualize, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createVisualizePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("Visualize");
        button.addActionListener(e -> {
            try {
                Document doc = function.createDocument(InputProcessor.process());
                doc.setCustom(true);
                GraphViewer.createViewer(WIDTH - 100, HEIGHT - 100, doc);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        panel.add(button);
        return panel;
    }

    private static MouseListener createOpenDocumentListener() {
        return new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();

                int row = table.getSelectedRow();
                if (mouseEvent.getClickCount() == 2 && row != -1) {
                    table.getSelectedRow();
                    Document doc = (Document) table.getValueAt(row, 0);

                    try {
                        Desktop.getDesktop().open(doc.getPath().toFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private static ActionListener createQuerySearchListener(JTextField textField, JTable table) {
        return (l) -> {
            List<Result> results = null;
            try {
                InputProcessor.setReader(new QueryReader(textField.getText()));
                results = function.process(InputProcessor.process());
            } catch (IOException ex) {
                GUIUtils.showErrorMessage(null, "Could not process query!");
                System.exit(1);
            }

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            results.forEach(result -> model.addRow(new Object[]{result.getDocument(), result.getSim()}));
        };
    }

    private static ActionListener createLoadDocumentListener(JLabel label, JTable table) {
        return (l) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Choose Document");

            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path document = chooser.getSelectedFile().toPath();
            label.setText(document.toString());

            List<Result> results = null;
            try {
                results = processDocument(document);
            } catch (IOException ex) {
                GUIUtils.showErrorMessage(null, "Could not process document!");
                System.exit(1);
            }

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            results.forEach(result -> model.addRow(new Object[]{result.getDocument(), result.getSim()}));
        };
    }

    private JTable createTable() {
        JTable table = new JTable(new DefaultTableModel(new Object[]{"Document", "Similarity"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.addMouseListener(createOpenDocumentListener());
        return table;
    }

    private JPanel createPanel2Form(JTable table) {
        JPanel form = new JPanel(new GridLayout(1, 3));

        JLabel label = new JLabel("Query");
        form.add(label);

        JTextField textField = new JTextField();
        textField.addActionListener(createQuerySearchListener(textField, table));
        form.add(textField);

        JButton button = new JButton("Search");
        button.addActionListener(createQuerySearchListener(textField, table));
        form.add(button);

        form.setBorder(BorderFactory.createTitledBorder("Enter Query"));
        return form;
    }

    private JPanel createPanel3Form(JTable table) {
        JPanel form = new JPanel(new GridLayout(1, 3));

        JLabel label = new JLabel("Document");
        form.add(label);

        JLabel docLabel = new JLabel("No document selected.");
        form.add(docLabel);

        JButton button = new JButton("Browse...");

        button.addActionListener(createLoadDocumentListener(docLabel, table));
        form.add(button);

        form.setBorder(BorderFactory.createTitledBorder("Enter Query"));
        return form;
    }

    // end of GUI initialization

    /**
     * Processes the user Document input and returns the list of results.
     *
     * @return list of results
     */
    private static List<Result> processDocument(Path document) throws IOException {
        InputProcessor.setReader(new PDFReader(document));
        return function.process(InputProcessor.process());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
