
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class Java_Port_Scanner extends JFrame {

    private final JTextField hostField =
            new JTextField("", 16);

    private final JTextField startField =
            new JTextField("", 6);

    private final JTextField endField =
            new JTextField("", 6);

    private final JTextArea resultsArea = new JTextArea();

    private final JProgressBar progressBar =
            new JProgressBar(0, 100);

    private final JLabel statusLabel =
            new JLabel("Ready to scan");

    private JButton scanButton;
    private JButton cancelButton;

    private SwingWorker<Void, String> scanWorker;

    public Java_Port_Scanner() {

        setTitle("Java Port Scanner");
        setSize(750, 570);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        mainPanel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        mainPanel.setBackground(
                new Color(18, 24, 35)
        );

        setContentPane(mainPanel);

        // Header
        JLabel title = new JLabel("JAVA PORT SCANNER");

        title.setForeground(
                new Color(0, 220, 255)
        );

        title.setFont(
                new Font("Arial", Font.BOLD, 24)
        );

        JPanel header = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        header.setBackground(
                new Color(18, 24, 35)
        );

        header.add(title);

        mainPanel.add(header, BorderLayout.NORTH);

        // Input fields
        JPanel inputPanel = new JPanel(
                new GridLayout(3, 2, 10, 10)
        );

        inputPanel.setBackground(
                new Color(25, 33, 47)
        );

        inputPanel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        inputPanel.add(
                createLabel("Target IP / Hostname:")
        );
        inputPanel.add(hostField);

        inputPanel.add(createLabel("Start Port:"));
        inputPanel.add(startField);

        inputPanel.add(createLabel("End Port:"));
        inputPanel.add(endField);

        JPanel centerPanel = new JPanel(
                new BorderLayout(8, 8)
        );

        centerPanel.setBackground(
                new Color(18, 24, 35)
        );

        centerPanel.add(
                inputPanel, BorderLayout.NORTH
        );

        // Results area
        resultsArea.setEditable(false);

        resultsArea.setFont(
                new Font("Monospaced", Font.PLAIN, 13)
        );

        resultsArea.setBackground(
                new Color(10, 15, 23)
        );

        resultsArea.setForeground(
                new Color(100, 255, 150)
        );

        resultsArea.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(
                resultsArea
        );

        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(
                                new Color(0, 180, 220)
                        ),
                        "Scan Results"
                )
        );

        centerPanel.add(
                scrollPane, BorderLayout.CENTER
        );

        mainPanel.add(
                centerPanel, BorderLayout.CENTER
        );

        // Buttons: Start Scan and Cancel only
        scanButton = new JButton("Start Scan");
        cancelButton = new JButton("Cancel");

        cancelButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        buttonPanel.setBackground(
                new Color(18, 24, 35)
        );

        buttonPanel.add(scanButton);
        buttonPanel.add(cancelButton);

        // Footer
        JPanel footer = new JPanel(
                new BorderLayout(5, 5)
        );

        footer.setBackground(
                new Color(18, 24, 35)
        );

        footer.add(
                buttonPanel, BorderLayout.NORTH
        );

        footer.add(
                progressBar, BorderLayout.CENTER
        );

        footer.add(
                statusLabel, BorderLayout.SOUTH
        );

        mainPanel.add(
                footer, BorderLayout.SOUTH
        );

        // Button actions
        scanButton.addActionListener(
                e -> startScan()
        );

        cancelButton.addActionListener(e -> {
            if (scanWorker != null
                    && !scanWorker.isDone()) {

                scanWorker.cancel(true);
                statusLabel.setText(
                        "Cancelling scan..."
                );
            }
        });
    }

    private JLabel createLabel(String text) {

        JLabel label = new JLabel(text);

        label.setForeground(Color.WHITE);

        label.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        return label;
    }

    private void startScan() {

        final String host =
                hostField.getText().trim();

        final int startPort;
        final int endPort;

        try {
            startPort = Integer.parseInt(
                    startField.getText().trim()
            );

            endPort = Integer.parseInt(
                    endField.getText().trim()
            );

            if (host.isEmpty()
                    || startPort < 1
                    || endPort > 65535
                    || startPort > endPort) {

                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid host and port range (1-65535).",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        int totalPorts = endPort - startPort + 1;

        resultsArea.setText("");
        progressBar.setValue(0);

        statusLabel.setText(
                "Scanning " + host + "..."
        );

        scanButton.setEnabled(false);
        cancelButton.setEnabled(true);

        resultsArea.append(
                "Target: " + host + "\n"
        );

        resultsArea.append(
                "Port range: " + startPort
                        + "-" + endPort + "\n\n"
        );

        scanWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground()
                    throws Exception {

                ExecutorService pool =
                        Executors.newFixedThreadPool(100);

                CompletionService<String> completion =
                        new ExecutorCompletionService<>(pool);

                int submitted = 0;
                int completed = 0;

                try {
                    // Submit port checks
                    for (int port = startPort;
                         port <= endPort && !isCancelled();
                         port++) {

                        final int currentPort = port;

                        completion.submit(() -> {

                            try (Socket socket = new Socket()) {

                                socket.connect(
                                        new InetSocketAddress(
                                                host,
                                                currentPort
                                        ),
                                        500
                                );

                                return currentPort
                                        + "/tcp OPEN";

                            } catch (Exception ex) {
                                return null;
                            }
                        });

                        submitted++;
                    }

                    // Collect scan results
                    while (completed < submitted
                            && !isCancelled()) {

                        Future<String> future =
                                completion.poll(
                                        100,
                                        TimeUnit.MILLISECONDS
                                );

                        if (future == null) {
                            continue;
                        }

                        completed++;

                        String result = future.get();

                        if (result != null) {
                            publish(result);
                        }

                        setProgress(
                                (int) (
                                        completed * 100.0
                                                / totalPorts
                                )
                        );
                    }

                } finally {
                    pool.shutdownNow();
                }

                return null;
            }

            @Override
            protected void process(List<String> lines) {

                for (String line : lines) {
                    resultsArea.append(line + "\n");
                }
            }

            @Override
            protected void done() {

                scanButton.setEnabled(true);
                cancelButton.setEnabled(false);

                if (isCancelled()) {

                    statusLabel.setText(
                            "Scan cancelled."
                    );

                } else {

                    progressBar.setValue(100);

                    statusLabel.setText(
                            "Scan completed."
                    );

                    resultsArea.append(
                            "\nScan completed.\n"
                    );
                }
            }
        };

        scanWorker.addPropertyChangeListener(event -> {

            if ("progress".equals(
                    event.getPropertyName()
            )) {

                progressBar.setValue(
                        (Integer) event.getNewValue()
                );
            }
        });

        scanWorker.execute();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Java_Port_Scanner app =
                    new Java_Port_Scanner();

            app.setVisible(true);
        });
    }
}
