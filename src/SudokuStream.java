// Import necessary libraries
import org.opencv.core.Point; // OpenCV library for defining points
import org.opencv.core.*; // OpenCV core functionalities
import org.opencv.highgui.HighGui; // OpenCV high-level GUI functions
import org.opencv.imgcodecs.Imgcodecs; // OpenCV image I/O functions
import org.opencv.imgproc.Imgproc; // OpenCV image processing functions
import org.opencv.videoio.VideoCapture; // OpenCV library for capturing video
import javax.swing.*; // Java Swing library for GUI components
import java.awt.*; // Abstract Window Toolkit for GUI
import java.awt.event.*; // Java AWT event handling
import javax.swing.filechooser.FileNameExtensionFilter; // File chooser filter
import java.io.File; // Java File class for file operations

// This class represents the main JFrame for the SudokuStream application.
// This class "extends JFrame", meaning it is inherited from the JFrame class (uses properties within JFrame).

public class SudokuStream extends JFrame {

    // VideoCapture object for video input
    static VideoCapture videoCapture;

    // JPanel variables for UI components
    JPanel mainPanel;
    JPanel videoPanel;
    JPanel actionPanel;

    // JLabels for UI buttons and labels
    JLabel solveBtn;
    JLabel imgOptionBtn;
    JLabel vidOptionBtn;
    JLabel solvedLabel;
    JLabel infoLabel;

    // Image object for displaying processed frames
    static Image img;

    // Mat objects for storing video frames and processed frames
    static Mat frame;
    static Mat previousFrameClone;
    static Mat newFrame;

    // Timer for UI updates
    Timer timer;

    // JLabel for displaying "Solving..." message during processing
    JLabel solvingLabel;

    // DrawingPanel for displaying images
    DrawingPanel dp;

    // Boolean flag for video capture loop
    static boolean keepRunning = true;

    // Constructor for the SudokuStream class
    public SudokuStream() {
        super("Sudoku Stream"); // Set JFrame title

        // Initialize main panel with GridBagLayout
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit application on window close

        Dimension screenSize = new Dimension(1200, 900); // Set screen dimensions
        setPreferredSize(screenSize);

        // Set up panels for video display and action buttons
        videoPanel = new JPanel();
        actionPanel = new JPanel();

        // Set up info label for instructions
        infoLabel = new JLabel("<html><div style='text-align: center;'> <b>Welcome to SudokuSolver!</b> <br><br> If you already have an image you'd like to solve, " +
                "please click the Image Button.<br><br> If you'd like to take a video capture of a puzzle to solve, " +
                "please click the Video Button, and have the proper lighting for the camera to detect." +
                "</div></html>");
        infoLabel.setFont(new Font("Roboto Th", Font.PLAIN, 35));
        infoLabel.setPreferredSize(new Dimension(1000, 400));
        infoLabel.setForeground(Color.WHITE);

        // Set GridBagConstraints for info label
        GridBagConstraints gbcInfoLabel = new GridBagConstraints();
        gbcInfoLabel.anchor = GridBagConstraints.CENTER;

        // Add info label to main panel
        mainPanel.add(infoLabel, gbcInfoLabel);

        // Create image icons for buttons
        ImageIcon btn1 = new ImageIcon("buttons/btn1.png");
        ImageIcon btn1Hover = new ImageIcon("buttons/btn1Hover.png");
        ImageIcon btn2 = new ImageIcon("buttons/btn2.png");
        ImageIcon btn2Hover = new ImageIcon("buttons/btn2Hover.png");

        // Create and add image option button
        imgOptionBtn = createHoverLabel(btn1, btn1Hover, "img");
        vidOptionBtn = createHoverLabel(btn2, btn2Hover, "vid");

        // Add video panel and action panel to main panel
        actionPanel.add(imgOptionBtn);
        actionPanel.add(vidOptionBtn);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; // Give weight to expand horizontally
        gbc.weighty = 0.9; // Give weight to expand vertically
        mainPanel.add(videoPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.1;
        mainPanel.add(actionPanel, gbc);


        add(mainPanel);

        pack(); // Establish all parameters and preferred sizes so content fits in the JFrame

        videoPanel.setBackground(new Color(46, 43, 34));
        actionPanel.setBackground(new Color(33, 36, 34));
        setForeground(Color.BLACK);

        setLocationRelativeTo(null); // Center the frame on the screen
        setVisible(true); // Enable components to be viewed

    }

    private JLabel createHoverLabel(ImageIcon icon, ImageIcon hoverIcon, String btnType) {
        JLabel label = new JLabel();
        label.setIcon(icon);

        // Add a MouseAdapter to the label
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // This method is called when the mouse enters the label
                label.setIcon(hoverIcon); // Change label text color when hovered
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Set hand cursor
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // This method is called when the mouse exits the label
                label.setIcon(icon); // Restore label text color when not hovered
                label.setCursor(Cursor.getDefaultCursor()); // Restore default cursor

            }

            public void mousePressed(MouseEvent e) {
                // This method is called when the label is clicked
                if (btnType.equals("img")) {
                    imgButtonClicked();
                } else if (btnType.equals("vid")) {
                    actionPanel.remove(imgOptionBtn);
                    actionPanel.remove(vidOptionBtn);
                    mainPanel.remove(infoLabel);

                    ImageIcon solveIcon = new ImageIcon("buttons/solveBtn.png");
                    ImageIcon solveIconHover = new ImageIcon("buttons/solveBtnHover.png");


                    // Add button which stops capture on click
                    solveBtn = createHoverLabel(solveIcon, solveIconHover, "solve");

                    actionPanel.add(solveBtn);


                    repaint();
                    pack();

                    // Once the JFrame is set up, start the video thread to run simultaneously with the Event Dispatch Thread
                    // used by Java Swing.
                    startVideoThread();

                } else {
                    keepRunning = false;
                    stopBtnClicked(); // If the button is clicked, follow the method
                }

            }
        });

        return label;
    }

    // Inner class for custom drawing
    private static class DrawingPanel extends JPanel {

        private Image imageToDraw;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Custom drawing code here
            g.drawImage(imageToDraw, 0, 0, null); // Display image on the JFrame using Graphics

        }

        // Method to set the image
        public void setImage(Image image) {
            this.imageToDraw = image;

            repaint(); // Trigger repaint to update the displayed image
        }

    }

    public void imgButtonClicked() {
        JFileChooser jf = new JFileChooser();

        jf.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Sudoku Images (png, jpg, pdf)", "png", "jpg", "pdf");
        jf.addChoosableFileFilter(filter);

        jf.showOpenDialog(null);

        File chosenFile = jf.getSelectedFile();

        if (chosenFile != null) {
            // Remove existing components
            actionPanel.remove(imgOptionBtn);
            actionPanel.remove(vidOptionBtn);
            mainPanel.remove(infoLabel);

            intermediateSolvingScreen();

            // Execute CPU-intensive methods in a separate thread
            new Thread(() -> {
                // Process the chosen image
                String filePath = chosenFile.getAbsolutePath();
                Mat inputMat = Imgcodecs.imread(filePath);
                Mat cornerDst = HoughLinesRun.run(inputMat);

                // Perform post-processing
                postProcessing(inputMat, cornerDst);

                // Stop the timer and remove the "Solving..." label after processing is complete
                timer.stop();
                mainPanel.remove(solvingLabel);

                // Revalidate and repaint the main panel to update the UI
                mainPanel.revalidate();
                mainPanel.repaint();
            }).start();

        }

    }

    public void intermediateSolvingScreen() {
        // Set the background color of the main panel to black
        mainPanel.setBackground(Color.BLACK);

        // Create and add the "Solving..." label
        solvingLabel = new JLabel("Solving...");
        solvingLabel.setFont(new Font("Georgia", Font.BOLD, 40));
        solvingLabel.setForeground(Color.WHITE); // Set text color to white
        GridBagConstraints gbcSolvingLabel = new GridBagConstraints();
        gbcSolvingLabel.gridx = 0;
        gbcSolvingLabel.gridy = 0;
        gbcSolvingLabel.anchor = GridBagConstraints.CENTER;
        mainPanel.add(solvingLabel, gbcSolvingLabel);

        // Schedule a timer to update the text of the label at regular intervals
        timer = new Timer(500, e -> {
            String text = solvingLabel.getText();
            if (text.equals("Solving...")) {
                solvingLabel.setText("Solving");
            } else {
                solvingLabel.setText(text + ".");
            }
        });
        timer.start();

        // Revalidate and repaint the main panel to update the UI
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // Method triggered when stop button is clicked
    public void stopBtnClicked() {
        videoCapture.release(); // Release video capture resources
        actionPanel.remove(solveBtn); // Remove solve button

        // Remove existing components
        actionPanel.remove(imgOptionBtn);
        actionPanel.remove(vidOptionBtn);
        mainPanel.remove(infoLabel);

        try {
            Thread.sleep(1000); // Pause execution for 1 second
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        intermediateSolvingScreen(); // Display intermediate solving screen

        // Execute CPU-intensive methods in a separate thread
        new Thread(() -> {
            // Process the chosen image

            // Perform post-processing
            postProcessing(frame, newFrame);

            // Stop the timer and remove the "Solving..." label after processing is complete
            timer.stop();
            mainPanel.remove(solvingLabel);

            // Revalidate and repaint the main panel to update the UI
            mainPanel.revalidate();
            mainPanel.repaint();
        }).start();

    }

    // Method to perform post-processing on the image
    public void postProcessing(Mat src, Mat procImg) {
        mainPanel.remove(actionPanel);
        mainPanel.remove(videoPanel);

        pack(); // Pack components within JFrame

        Mat warpedMat = warpMat(src, procImg); // Warp input image
        Imgcodecs.imwrite("data/images/stream_img.jpg", warpedMat); // Write processed image to file

        Mat solvedClone = warpedMat.clone(); // Clone warped image
        HoughLinesRun.removeLines(warpedMat); // Remove lines from image
        Imgcodecs.imwrite("data/images/rmv.jpg", warpedMat); // Write processed image to file
        HoughLinesRun.outputCells(warpedMat); // Output cells from image
        solveSudoku(solvedClone); // Solve Sudoku puzzle
    }

    // Method to solve the Sudoku puzzle
    public void solveSudoku(Mat solvedClone) {
        SudokuSolver.main(); // Run Sudoku solver
        Mat solvedMat = SudokuSolver.drawSolutions(solvedClone); // Draw Sudoku solutions
        refactorJFrame(solvedMat); // Refactor JFrame to display solved puzzle
    }

    // Method to refactor JFrame to display solved puzzle
    public void refactorJFrame(Mat solvedMat) {
        img = HighGui.toBufferedImage(solvedMat); // Convert Mat to BufferedImage

        int imgWidth = solvedMat.width();
        int imgHeight = solvedMat.height();

        // Set new screen dimensions
        Dimension newScreenSize = new Dimension(imgWidth, imgHeight + 120);
        setPreferredSize(newScreenSize);

        // Create label indicating puzzle status
        if (SudokuSolver.isPuzzleSolved) {
            solvedLabel = new JLabel("Solved Sudoku Puzzle!");
        } else {
            solvedLabel = new JLabel("<html>Puzzle not solved!<br>Please try again.</html>");
        }
        solvedLabel.setFont(new Font("Georgia", Font.BOLD, 40));

        dp = new DrawingPanel();
        dp.setImage(img);

        // Add the drawing panel with constraints
        GridBagConstraints gbcDrawingPanel = new GridBagConstraints();
        gbcDrawingPanel.gridx = 0;
        gbcDrawingPanel.gridy = 1;
        gbcDrawingPanel.gridwidth = GridBagConstraints.REMAINDER;
        gbcDrawingPanel.fill = GridBagConstraints.BOTH;
        gbcDrawingPanel.weightx = 1.0;
        gbcDrawingPanel.weighty = 0.98;
        mainPanel.add(dp, gbcDrawingPanel);

        JPanel solvedPanel = new JPanel();

        // Add the info label with constraints
        GridBagConstraints gbcSolvedPanel = new GridBagConstraints();
        gbcSolvedPanel.gridx = 0;
        gbcSolvedPanel.gridy = 0;
        gbcSolvedPanel.fill = GridBagConstraints.BOTH;
        gbcSolvedPanel.weightx = 1.0; // Make it expand to the full width
        gbcSolvedPanel.weighty = 0.02;
        gbcSolvedPanel.anchor = GridBagConstraints.CENTER;
        mainPanel.add(solvedPanel, gbcSolvedPanel);
        solvedPanel.setBackground(Color.DARK_GRAY);
        solvedPanel.add(solvedLabel);

        solvedLabel.setForeground(Color.WHITE);

        repaint();

        pack();


        setLocationRelativeTo(null); // Center the frame on the screen
    }

    // Method to warp input image
    public Mat warpMat(Mat img, Mat procImg) {
        Point[] sortedPoints = HoughLinesRun.sortedPoints; // Get sorted points
        Mat warpedMat = new Mat(procImg.height(), procImg.width(), procImg.type()); // Create new Mat

        // Calculate dimensions
        double x = sortedPoints[1].x - sortedPoints[0].x;
        double y = sortedPoints[2].y - sortedPoints[0].y;

        // Define destination points
        MatOfPoint2f cdst = new MatOfPoint2f(
                new Point(0,0),
                new Point(x, 0),
                new Point(0, y),
                new Point(x, y)
        );

        MatOfPoint2f src = new MatOfPoint2f(sortedPoints); // Source points
        Imgproc.warpPerspective(img, warpedMat, Imgproc.getPerspectiveTransform(src, cdst), new Size(x, y)); // Warp image

        return warpedMat; // Return warped image
    }

    // Method to start video capture thread
    public void startVideoThread() {
        videoCapture = new VideoCapture(0); // Initialize VideoCapture object

        if (keepRunning) {
            try {
                Thread.sleep(2000); // Pause execution for 2 seconds
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                frame = new Mat(); // Initialize frame Mat
            }
        }

        // Define videoThread
        Thread videoThread = new Thread(() -> {
            while (videoCapture.isOpened()) {
                previousFrameClone = frame; // Clone previous frame

                if (videoCapture.read(frame)) { // Read frame
                    videoCapture.read(frame); // Read frame again

                    newFrame = HoughLinesRun.run(frame); // Process frame

                    img = HighGui.toBufferedImage(newFrame); // Convert Mat to BufferedImage

                    // Update video panel with new frame
                    SwingUtilities.invokeLater(() -> {
                        Graphics g = videoPanel.getGraphics(); // Get Graphics object
                        if (g != null) {
                            g.clearRect(0, 0, getWidth(), getHeight()); // Clear previous drawing
                            g.drawImage(img, 0, 0, videoPanel.getWidth(), videoPanel.getHeight(), null); // Draw new frame
                        }
                    });
                }
            }
            videoCapture.release(); // Release video capture resources
        });
        videoThread.start(); // Start video capture thread
    }

    // Main method
    public static void main(String[] args) {
        // Load OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Invoke JFrame as its own thread
        SwingUtilities.invokeLater(SudokuStream::new);
    }

} // Class end
