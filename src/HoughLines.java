// This file performs the bulk of the image processing in the project.
// It takes the last frame of the SudokuStream and performs a Hough Transformation to detect
// edges and boundaries of the grid.
// From there, the grid is separated into many cells to be read in the next stage.

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;


/**
 Documentation for Computer Vision is sourced by OpenCV:
 <a href="https://docs.opencv.org/3.4/d9/db0/tutorial_hough_lines.html">...</a>
 */

// "Main" Class to be run
class HoughLinesRun {

    // Counter used to state which cell of the grid we are on during extraction
    static int cellNumber = 1;

    // "main" method
    public void run(String[] args) {

        // Declare output variables (these Mat [matrix] objects will be written into to produce output Mats).
        Mat dst = new Mat(), cdst = new Mat();

        String defaultFile = "FinalFrame.jpg"; // "FinalFrame.jpg" Path to the file (taken from the frame in SudokuStream)

        // If an argument is passed via the terminal, we use that as the file to be processed.
        // Else, we use the default file expressed earlier
        String filename = ((args.length > 0) ? args[0] : defaultFile);

        // Load image
        Mat src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE);

        // Check if image is loaded properly
        // If not, display the associated error message
        if (src.empty()) {
            System.out.println("Error opening image!");
            System.out.println("Program Arguments: [image_name -- default " + defaultFile + "] \n");
            System.exit(-1);

        }


        // Edge detection
        Imgproc.Canny(src, dst, 50, 200, 3, false);

        // Copy edges to image displaying results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLines(dst, lines, 1.08, Math.PI / 180, 175); // runs the actual detection

        // Draw the lines
        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0],
                    theta = lines.get(x, 0)[1];
            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a * rho, y0 = b * rho;
            Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));
            Imgproc.line(cdst, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }


        // Show Results in window
//        HighGui.imshow("Source", src);
//        HighGui.imshow("Binary Image Transform", cdst);

        // Write grayscale result in an image
        Imgcodecs.imwrite("grayscale.jpg", cdst);

        // Now with the grayscaled image, extract the cells of the grid
        extractCellsFromImage(cdst, src);

    } // run() method end


    // Method to separate the grid into its cells
    private static void extractCellsFromImage(Mat cdst, Mat src) {

        // Create variables to be modified throughout extraction process
        ArrayList<Mat> cells = new ArrayList<>(); // Appendable list of extracted cells (end product)
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> cellContours = new ArrayList<>(); // Contours of each cell used to distinguish a cell


        // Adaptive Threshold used to transform image into binary
        Imgproc.adaptiveThreshold(src, cdst, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 20);


        // Find the contours that distinguish the cell into its bounding rectangle
        Imgproc.findContours(cdst, cellContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Write the processed image into binary.jpg
        Imgcodecs.imwrite("binary.jpg", cdst);


        // Loop through the contours in the updated list
        for (MatOfPoint contour : cellContours) {

            // Create a Rect obj of the bounding rectangle of the cell
            Rect rect = Imgproc.boundingRect(contour);

            // Filter the rectangles to the following parameters: Width/Height > 15 and Area < 2000
            if (rect.width > 17 && rect.height > 17 && rect.area() < 2000) {

                // If rectangle passes the constraints, extract the cell from the image based on the dimensions
                // and location of the rectangle.
                Mat cell = new Mat(cdst, rect);

                // Add the extracted cell to the cells list
                cells.add(cell);

            }
        }

        // Loop through the cells in the updated list
        for (Mat cell : cells) {

            // Write each extracted cell into an image with its corresponding cell number
            // NOTE: Images are found in the data directory
            Imgcodecs.imwrite("data/Cell_" + cellNumber + ".jpg", cell);
            cellNumber++;

        }

        System.out.println("Done Binary Image");

    } // Extraction method end

}


// Class to call the "main" class
public class HoughLines {

    public static void main(String[] args) {

        // Load native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Run main class
        new HoughLinesRun().run(args);

    }

    // Boolean method to move on to the next component of the project in SudokuCV.java
    // only when all the cells are extracted (81)
    public static boolean areCellsExtracted() {

        return HoughLinesRun.cellNumber >= 81;
    }
}