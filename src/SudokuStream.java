import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

// This class "extends JFrame", meaning it is inherited from the JFrame class (uses properties within JFrame).
// It also implements ActionListener and MouseListener, which means that the methods inherited from the Listeners must be
// "implemented" (modified / called) in the class.

public class SudokuStream extends JFrame implements ActionListener, MouseListener {

    // Initialize VideoCapture obj.
    static VideoCapture videoCapture;

    // Initialize JFrame Variables
    JPanel videoPanel;
    JPanel otherPanels;
    JButton quitBtn;

    // Initialize output Mat variables
    Mat dst, cdst, cdstP;

    // Initialize Mat frames
    // "frame" --> input source frame
    // "frame img" --> output frame to be processed in the display
    Mat frame, frameImg;

    // Counter for how many times the user has clicked (used to detect corners of image to crop)
    int pointerClick = 0;

    // Initialize points used for the corners of the cropped image
    Point[] sortedPoints = new Point[4];

    public SudokuStream() {
        super("Sudoku Stream"); // Title of the JFrame window

        setLayout(null); // No Layout (messes with the video stream)


        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Simply close the window when closing JFrame

        Dimension screenSize = new Dimension(1200, 900); // Dimensions of Screen
        setPreferredSize(screenSize);


        videoCapture = new VideoCapture(0); // Change 0 to your video source

        // Set up parameters for panel to display video stream
        videoPanel = new JPanel();
        videoPanel.setBounds(0, 0, 1200, 700);
        add(videoPanel);

        // Set up JPanel for other elements (stop button)
        otherPanels = new JPanel();
        otherPanels.setPreferredSize(new Dimension(100, 200));
        otherPanels.setBorder(new LineBorder((Color.BLUE)));
        otherPanels.setBounds(350, 700, 500, 200);

        // Add button which stops capture on click
        quitBtn = new JButton("STOP");
        otherPanels.add(quitBtn);

        // Listen for button click
        quitBtn.addActionListener(this); // "this" keyword indicates the object of this class

        add(otherPanels); // add to JFrame

        getContentPane().addMouseListener(this); // Listen for mouse click

        pack(); // Establish all parameters and preferred sizes so content fits in the JFrame

        setVisible(true); // Enable components to be viewed


        // Once the JFrame is set up, start the video thread to run simultaneously with the Event Dispatch Thread
        // used by Java Swing.
        startVideoThread();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == quitBtn) {
            quitBtnClicked(); // If the button is clicked, follow the method
        }
    }

    public void quitBtnClicked() {
        videoCapture.release(); // end video capture

        otherPanels.remove(quitBtn); // remove button

        // Define the four corners of the rectangle based on user mouse clicks
        Point[] points = sortedPoints.clone();


        // Draw lines between the corners to form the rectangle
        for (int i = 0; i < 4; i++) {
            Imgproc.line(frame, points[i], points[(i + 1) % 4], new Scalar(0, 255, 0), 2);
        }

        // Define a bounding box around the rectangle
        Rect boundingBox = Imgproc.boundingRect(new MatOfPoint(points));

        // Crop the region of interest (ROI) from the image
        Mat croppedImage = new Mat(frame, boundingBox);

        // Write this Mat into the image FinalFrame.jpg to be used for image processing
        Imgcodecs.imwrite("FinalFrame.jpg", croppedImage);

        HighGui.destroyAllWindows(); // Close all windows
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x=e.getX();
        int y=e.getY();

        System.out.println("Pressed: " + x + y);

        if (pointerClick < 4) {
            sortedPoints[pointerClick] = new Point(x, y);
        }

        pointerClick++;
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    public void startVideoThread() {
        // videoThread definition
        Thread videoThread = new Thread(() -> {

            // Mat obj to retrieve the current frame of the video capture
            frame = new Mat();

            while (videoCapture.isOpened()) {
                if (videoCapture.read(frame)) {

                    // Get the Graphics of the videoPanel so that we can update the display with the current frame
                    Graphics g = videoPanel.getGraphics();
                    super.paintComponents(g);

                    // Read and update frame
                    videoCapture.read(frame);

                    // frameImg is to be processed during the display
                    // frame is to be sent to HoughLines.java for its own processing
                    frameImg = frame.clone();

                    // Assign output variables
                    dst = new Mat();
                    cdst = new Mat();

                    // Perform Canny on frame, output to dst
                    Imgproc.Canny(frameImg, dst, 50, 200, 3, false);

                    // Perform Grayscale on dst, output to cdst
                    Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

                    // assign cdstP as a cdst clone (for Probabilistic Hough Transform)
                    cdstP = cdst.clone();


                    // Probabilistic Line Transform
                    Mat linesP = new Mat(); // will hold the results of the detection
                    Imgproc.HoughLinesP(dst, linesP, 1, Math.PI / 180, 50, 50, 10); // runs the actual detection

                    // Draw the lines
                    for (int x = 0; x < linesP.rows(); x++) {
                        double[] l = linesP.get(x, 0);
                        Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
                    }


                    Image buffImg = HighGui.toBufferedImage(frameImg); // Convert frame Mat into image to be displayed

                    g.drawImage(buffImg, 0, 0, null); // Display image on the JFrame using Graphics

                }
            }
            videoCapture.release(); // Release video capture when done
        }); // videoThread end

        videoThread.start(); // Start the thread of the video capture
    }


    public static void main(String[] args) {

        // Load OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Invoke JFrame (Java Swing) as its own thread, allowing for the video thread to run later
        SwingUtilities.invokeLater(SudokuStream::new);
    } // end main


    // Boolean method to move on to the next component of the project in SudokuCV.java
    // only when video capture is closed
    public static boolean isVideoOpened() {
        return videoCapture.isOpened();
    }

} // Class end