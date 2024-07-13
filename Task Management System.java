import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class S25207P01 {
        public static void main(String[] args) {
            SwingUtilities.invokeLater(StartWindow::show);
        }
    }
    class StartWindow {
        public static void show() {
            JFrame frame = new JFrame("Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel contentPain = new JPanel();
            contentPain.setLayout(new BoxLayout(contentPain, BoxLayout.Y_AXIS));

            JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            JLabel label = new JLabel("Choose source root");

            northPanel.add(label);

            contentPain.add(northPanel);

            JPanel centerPanel = getCenterPanel(frame);
            contentPain.add(centerPanel);

            frame.getContentPane().add(contentPain);

            frame.setSize(800, 600);
            frame.setVisible(true);
        }

        private static JPanel getCenterPanel(JFrame frame) {
            JPanel centerPanel = new JPanel(new BorderLayout());

            JButton fileChooserButton = new JButton("Choose file");
            fileChooserButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    DataStorageUtil.getInstance().setRootFolder(fileChooser.getSelectedFile());
                    frame.dispose();
                    MainWindow.show();
                }
            });

            JPanel centerCenterPanel = new JPanel();
            centerCenterPanel.add(fileChooserButton);

            centerPanel.add(centerCenterPanel, BorderLayout.NORTH);
            return centerPanel;
        }
    }
     class MainWindow {

        public static void show() {
            JFrame frame2 = new JFrame("Frame 2");
            frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame2.setSize(800, 600);
            frame2.setVisible(true);
            frame2.getContentPane().add(getContentPane());
        }

        private static JPanel getContentPane() {
            JPanel mainPanel = new JPanel(new BorderLayout());

            EditorPanel topCenterPanel = new EditorPanel();

            FileTreePanel leftPanel = new FileTreePanel(topCenterPanel);
            leftPanel.setBackground(Color.RED);
            mainPanel.add(leftPanel, BorderLayout.WEST);

            JPanel centerPanel = new JPanel(new BorderLayout());

            topCenterPanel.setBackground(Color.GREEN);
            centerPanel.add(topCenterPanel, BorderLayout.CENTER);

            ControlPanel bottomCenterPanel = new ControlPanel(leftPanel);
            bottomCenterPanel.setBackground(Color.BLUE);
            centerPanel.add(bottomCenterPanel, BorderLayout.SOUTH);

            mainPanel.add(centerPanel, BorderLayout.CENTER);

            return mainPanel;
        }
    }
    class FileUtil {
        public static long countFilesInSubdirectories(Path directory){
            try (Stream<Path> subdirectories = Files.list(directory)) {
                return subdirectories
                        .filter(Files::isDirectory)
                        .flatMap(subdir -> {
                            try {
                                return Files.list(subdir);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(Files::isRegularFile)
                        .count();
            }catch(IOException e) {
                return -1;
            }
        }

        public static java.util.List<Path> getSolutionForStudentDirectory(Path directory){
            try (Stream<Path> subdirectories = Files.list(directory)) {
                return subdirectories
                        .filter(Files::isDirectory)
                        .flatMap(subdir -> {
                            try {
                                return Files.list(subdir);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(Files::isRegularFile)
                        .toList();
            }catch(IOException e) {
                return List.of();
            }
        }

        public static String getContentOfFileByPath(Path filePath){
            try {
                return Files.readString(filePath);
            } catch (IOException e) {
                return null;
            }
        }
    }
     class MyTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof File file) {
                setText(file.getName());
            }
            return this;
        }
    }
    class FileTreePanel extends JPanel {

        private final MyTreeCellRenderer myTreeCellRenderer = new MyTreeCellRenderer();

        private EditorPanel editorPanel;

        public FileTreePanel(EditorPanel editorPanel) {
            this.editorPanel = editorPanel;
            setLayout(new BorderLayout());
            addFileTree();
        }

        private void addFileTree(){
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(DataStorageUtil.getInstance().getRootFolder());

            DefaultTreeModel treeModel = new DefaultTreeModel(root);

            JTree fileTree = new JTree(treeModel);

            fileTree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                        if (path != null) {
                            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                            Object userObject = selectedNode.getUserObject();
                            if (userObject instanceof File selectedFile && selectedFile.isFile()) {
                                try {
                                    DataStorageUtil.getInstance().setCurrentOpenedFile(selectedFile);
                                    editorPanel.getTextArea().setText(Files.readString(selectedFile.toPath()));
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                /* textArea.setText("Double-clicked: " + selectedFile.getAbsolutePath());*/
                            }
                        }
                    }
                }
            });

            fileTree.setCellRenderer(myTreeCellRenderer);

            JScrollPane scrollPane = new JScrollPane(fileTree);

            add(scrollPane, BorderLayout.CENTER);

            populateFileTree(root, DataStorageUtil.getInstance().getRootFolder());

            expandAllNodes(fileTree, 0, fileTree.getRowCount());

        }

        private void populateFileTree(DefaultMutableTreeNode node, File file) {
            if (file.isDirectory()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
                node.add(childNode);
                if (file.listFiles() == null) return;
                Arrays.stream(file.listFiles()).forEach(f -> populateFileTree(childNode, f));
            } else {
                node.add(new DefaultMutableTreeNode(file));
            }
        }

        private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
            for (int i = startingIndex; i < rowCount; ++i) {
                tree.expandRow(i);
            }
            if (tree.getRowCount() != rowCount) {
                expandAllNodes(tree, rowCount, tree.getRowCount());
            }
        }

        public void readAndUpdateTree(){
            removeAll();
            addFileTree();
            revalidate();
            updateUI();
        }
    }
     class EditorPanel extends JPanel {
        private JTextArea textArea = new JTextArea();


        public EditorPanel() {
            setLayout(new BorderLayout());
            addScrollPaneWithEditor();
        }

        private void addScrollPaneWithEditor() {
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            add(scrollPane, BorderLayout.CENTER);
            JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener((e) -> {
                try {
                    Files.writeString(DataStorageUtil.getInstance().getCurrentOpenedFile().toPath(), textArea.getText());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            savePanel.add(saveButton);
            add(savePanel, BorderLayout.SOUTH);
        }

        public JTextArea getTextArea() {
            return textArea;
        }
    }
    class ControlPanel extends JPanel {

        private final JTextField studentNameField = new JTextField();
        private final JTextField studentSurnameField = new JTextField();
        private final AtomicInteger indexOfFile = new AtomicInteger(0);
        private final FileTreePanel fileTreePanel;

        public ControlPanel(FileTreePanel fileTreePanel) {
            this.fileTreePanel = fileTreePanel;
            JButton receivedSolutionButton = getReceivedSolutionButton();
            JButton solutionForStudentButton = new JButton("Solutions by student");
            JButton createTaskButton = createTaskButton();
            solutionForStudentButton.addActionListener((e) -> {
                SolutionsDialog solutionsDialog = new SolutionsDialog(studentNameField, studentSurnameField) {

                    @Override
                    protected JPanel southPanel() {
                        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                        JButton okButton = new JButton("OK");
                        okButton.addActionListener(e -> {
                            ResponseDialog dialog = new ResponseDialog();
                            Optional<File> studentDirectory = Arrays.stream(DataStorageUtil.getInstance().getRootFolder().listFiles()[0].listFiles())
                                    .filter(File::isDirectory)
                                    .filter(it -> it.getName().equals(studentNameField.getText() + " " + studentSurnameField.getText()))
                                    .findFirst();
                            List<Path> listOfPaths = FileUtil.getSolutionForStudentDirectory(studentDirectory.get().toPath());
                            dialog.addKeyListener(new KeyAdapter() {
                                @Override
                                public void keyPressed(KeyEvent e) {
                                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                                        if (indexOfFile.get() - 1 >= 0) {
                                            dialog.setContentPanel(dialog.getSolutionPanelForFile(FileUtil.getContentOfFileByPath(listOfPaths.get(indexOfFile.decrementAndGet()))));
                                        }

                                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                                        if (indexOfFile.get() + 1 < listOfPaths.size()) {
                                            dialog.setContentPanel(dialog.getSolutionPanelForFile(FileUtil.getContentOfFileByPath(listOfPaths.get(indexOfFile.incrementAndGet()))));
                                        }
                                    }
                                    dialog.revalidate();
                                    dialog.repaint();
                                }
                            });

                            dialog.setContentPanel(dialog.getSolutionPanelForFile(FileUtil.getContentOfFileByPath(listOfPaths.get(indexOfFile.get()))));
                            dialog.showDialog();

                        });
                        okPanel.add(okButton);
                        return okPanel;
                    }
                };
                solutionsDialog.showDialog();
            });
            add(receivedSolutionButton);
            add(solutionForStudentButton);
            add(createTaskButton);
        }

        private JButton getReceivedSolutionButton() {
            JButton receivedSolutionButton = new JButton("Received solutions");
            receivedSolutionButton.addActionListener(e -> {
                SolutionsDialog solutionsDialog = new SolutionsDialog(studentNameField, studentSurnameField) {

                    @Override
                    protected JPanel southPanel() {
                        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                        JButton okButton = new JButton("OK");
                        okButton.addActionListener(e -> {
                            ResponseDialog dialog = new ResponseDialog(studentNameField.getText() + " " + studentSurnameField.getText());
                            dialog.setContentPanel(dialog.getReceivedSolutionsPanel());
                            dialog.showDialog();

                        });
                        okPanel.add(okButton);
                        return okPanel;
                    }
                };
                solutionsDialog.showDialog();
            });
            return receivedSolutionButton;
        }

        private JButton createTaskButton() {
            JButton receivedSolutionButton = new JButton("Create task");
            receivedSolutionButton.addActionListener(e -> {
                CreateTaskDialog solutionsDialog = new CreateTaskDialog(fileTreePanel);
                solutionsDialog.showDialog();
            });
            return receivedSolutionButton;
        }
    }
     abstract class SolutionsDialog extends JDialog {

        private final JTextField studentNameField;
        private final JTextField studentSurnameField;

        public SolutionsDialog(JTextField studentNameField, JTextField studentSurnameField) {
            this.studentNameField = studentNameField;
            this.studentSurnameField = studentSurnameField;
            setTitle("Received solutions by student");
            setSize(600, 400);
            setLocationRelativeTo(null);
            getContentPane().add(getMainPanel());
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }

        public void showDialog() {
            setVisible(true);
        }

        private JPanel getMainPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout());

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

            JPanel studentNamePanel = new JPanel();

            studentNamePanel.add(new JLabel("Student name"));
            studentNameField.setColumns(20);
            studentNamePanel.add(studentNameField);

            JPanel studentSurnamePanel = new JPanel();


            studentSurnamePanel.add(new JLabel("Student surname"));
            studentSurnameField.setColumns(20);
            studentSurnamePanel.add(studentSurnameField);

            northPanel.add(studentNamePanel);
            northPanel.add(studentSurnamePanel);

            mainPanel.add(northPanel, BorderLayout.NORTH);

            JPanel okPanel = southPanel();

            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.add(okPanel, BorderLayout.SOUTH);
            return mainPanel;
        }

        protected abstract JPanel southPanel();
    }
     class ResponseDialog extends JDialog {

        private final JLabel responseLabel = new JLabel();

        private String nameAndSurname;

        public ResponseDialog(String nameAndSurname) {
            this();
            this.nameAndSurname = nameAndSurname;
        }

        public ResponseDialog() {
            setTitle("Response");
            setSize(600, 400);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }

        public JPanel getReceivedSolutionsPanel() {
            Optional<File> studentDirectory = Arrays.stream(DataStorageUtil.getInstance().getRootFolder().listFiles()[0].listFiles())
                    .filter(File::isDirectory)
                    .filter(it -> it.getName().equals(nameAndSurname))
                    .findFirst();
            if (studentDirectory.isEmpty()){
                responseLabel.setText("No student found by provided name and surname");
            }else {
                long numberOfSolution = FileUtil.countFilesInSubdirectories(studentDirectory.get().toPath());
                responseLabel.setText( "Solutions received by " + nameAndSurname + " = " + numberOfSolution);
            }
            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            centerPanel.add(responseLabel);
            return centerPanel;
        }

        public JPanel getSolutionPanelForFile(String s) {
            JPanel centerPanel = new JPanel(new BorderLayout());
            if (s.isEmpty()){
                responseLabel.setText("No student found by provided name and surname");
                centerPanel.add(responseLabel, BorderLayout.CENTER);
            }else {
                JTextArea solution = new JTextArea();
                solution.setEditable(false);
                solution.setLineWrap(true);
                solution.setWrapStyleWord(true);
                solution.setText(s);
                solution.setFocusable(false);
                centerPanel.add(solution, BorderLayout.CENTER);
            }
            return centerPanel;
        }

        public void showDialog(){
            setVisible(true);
        }


        public void setContentPanel(JPanel contentPanel) {
            getContentPane().removeAll();
            getContentPane().add(contentPanel);
        }
    }
    class CreateTaskDialog extends JDialog {

        private final JTextField taskNameField = new JTextField();

        private final FileTreePanel fileTreePanel;

        public CreateTaskDialog( FileTreePanel fileTreePanel) {
            this.fileTreePanel = fileTreePanel;
            setTitle("Create task");
            setSize(600, 400);
            setLocationRelativeTo(null);
            getContentPane().add(getMainPanel());
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }

        private JPanel getMainPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout());

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

            JPanel taskNamePanel = new JPanel();

            taskNamePanel.add(new JLabel("Task Name"));
            taskNameField.setColumns(20);
            taskNamePanel.add(taskNameField);

            northPanel.add(taskNamePanel);

            mainPanel.add(northPanel, BorderLayout.NORTH);

            JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            JButton okButton = new JButton("OK");
            okButton.addActionListener((e) -> {
                try {
                    Files.createDirectories( DataStorageUtil.getInstance().getRootFolder().listFiles()[1].toPath().resolve(taskNameField.getText()));

                    Arrays.stream(DataStorageUtil.getInstance().getRootFolder().listFiles()[0].listFiles()).forEach(folder -> {
                        try {
                            Files.createDirectories( folder.toPath().resolve(taskNameField.getText()));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    fileTreePanel.readAndUpdateTree();
                    dispose();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            okPanel.add(okButton);


            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.add(okPanel, BorderLayout.SOUTH);
            return mainPanel;
        }

        public void showDialog(){
            setVisible(true);
        }

    }
     class DataStorage {

        private File rootFolder;

        private File currentOpenedFile;

        public File getRootFolder() {
            return rootFolder;
        }

        public void setRootFolder(File rootFolder) {
            this.rootFolder = rootFolder;
        }

        public File getCurrentOpenedFile() {
            return currentOpenedFile;
        }

        public void setCurrentOpenedFile(File currentOpenedFile) {
            this.currentOpenedFile = currentOpenedFile;
        }
    }
     class DataStorageUtil {

        private static DataStorage dataStorage;


        private DataStorageUtil() {}

        public static DataStorage getInstance() {
            if (dataStorage == null) {
                dataStorage = new DataStorage();
            }
            return dataStorage;
        }
    }

