// This file performs the bulk of the image processing in the project.
// It takes the last frame of the SudokuStream and performs a Hough Transformation to detect
// edges and boundaries of the grid.
// From there, the grid is separated into many cells to be read in the next stage.

// Import necessary libraries
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;

/**
 * Documentation for Computer Vision is sourced by OpenCV:
 * <a href="https://docs.opencv.org/3.4/d9/db0/tutorial_hough_lines.html">...</a>
 */

// "Main" Class to be run
class HoughLinesRun {
    static Point[] sortedPoints = new Point[4]; // Array to hold sorted corner points of the Sudoku grid

    // Main method to process the frame and detect the Sudoku grid
    public static Mat run(Mat frame, String... args) {

        // Declare output variables (these Mat [matrix] objects will be written into to produce output Mats).
        Mat src = new Mat(); // Source image
        Mat dst = new Mat(); // Destination image after processing

        // Load image
        // If the source image is empty and video capture has stopped, use the previous frame
        if (src.empty() && !SudokuStream.keepRunning) {
            frame = SudokuStream.previousFrameClone;
        }

        // Convert the frame to grayscale
        try {
            Imgproc.cvtColor(frame, src, Imgproc.COLOR_BGR2GRAY);
        } catch (CvException e) {
            System.out.println("Image not rendered properly! Try again.");
            System.exit(-1);
        }


        // Invert the grayscale image
        Core.bitwise_not(src, dst);

        // Perform edge detection using the Canny algorithm
        Imgproc.Canny(dst, dst, 150, 200, 3, false);

        // List to hold the contours of each cell in the Sudoku grid
        ArrayList<MatOfPoint> cellContours = new ArrayList<>();

        // Find the contours that distinguish each cell into its bounding rectangle
        Imgproc.findContours(dst, cellContours, new Mat(dst.size(), dst.type()), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        double maxArea = 0;
        int maxIndex = 0;

        // Find the largest contour which should be the Sudoku grid
        for (int i = 0; i < cellContours.size(); i++) {
            final double contourArea = Imgproc.contourArea(cellContours.get(i));

            if (contourArea > maxArea) {
                maxArea = contourArea;
                maxIndex = i;
            }
        }

        // Convert the grayscale image back to BGR for color drawing
        Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2BGR);

        // If no contours are found, return the original frame
        if (cellContours.isEmpty()) {
            return frame;
        }

        // Draw the largest contour (the Sudoku grid) on the source image
        Imgproc.drawContours(src, cellContours, maxIndex, new Scalar(255, 0, 0), 3);

        // Get the corner points of the Sudoku grid
        final Point[] points = mapPoints(cellContours.get(maxIndex), src);

        // Draw markers on the corner points of the Sudoku grid
        for (final Point point : points) {
            if (point == null) {
                return frame;
            }
            Imgproc.drawMarker(src, point, new Scalar(0, 0, 255), 2, 30, 6);
        }

        return src;
    }

    // Approximate the polygonal curves of the contour to a more simplified version
    private static MatOfPoint2f approxPoly(MatOfPoint polygon) {
        final MatOfPoint2f src = new MatOfPoint2f();
        final MatOfPoint2f dst = new MatOfPoint2f();

        polygon.convertTo(src, CvType.CV_32FC2);

        // Approximate the polygonal curves
        Imgproc.approxPolyDP(src, dst, 0.02 * Imgproc.arcLength(src, true), true);

        return dst;
    }

    // Map the points of the contour to the corners of the Sudoku grid
    public static Point[] mapPoints(MatOfPoint polygon, Mat src) {
        MatOfPoint2f approxPoly = approxPoly(polygon);

        // If the approximated polygon does not have 4 points, return the existing sorted points
        if (!approxPoly.size().equals(new Size(1, 4))) {
            return sortedPoints;
        }

        // Calculate the center of mass of the approximated polygon
        final Moments moment = Imgproc.moments(approxPoly);
        final int centerX = (int) (moment.get_m10() / moment.get_m00());
        final int centerY = (int) (moment.get_m01() / moment.get_m00());

        // Sort corner points in reference to the center points
        for (int i = 0; i < approxPoly.rows(); i++) {
            final double[] data = approxPoly.get(i, 0); // Get the coordinates of the current point
            final double dataX = data[0]; // X-coordinate of the current point
            final double dataY = data[1]; // Y-coordinate of the current point

            // Determine which quadrant the point belongs to relative to the center point
            if (dataX < centerX && dataY < centerY) {
                // Top-left quadrant
                sortedPoints[0] = new Point(dataX, dataY);
            } else if (dataX > centerX && dataY < centerY) {
                // Top-right quadrant
                sortedPoints[1] = new Point(dataX, dataY);
            } else if (dataX < centerX && dataY > centerY) {
                // Bottom-left quadrant
                sortedPoints[2] = new Point(dataX, dataY);
            } else if (dataX > centerX && dataY > centerY) {
                // Bottom-right quadrant
                sortedPoints[3] = new Point(dataX, dataY);
            }
        }

        return sortedPoints;
    }

    // Remove lines from the Sudoku grid to isolate individual cells
    static void removeLines(final Mat src) {
        final Mat lines = new Mat();

        // Convert the image to grayscale
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

        // Apply adaptive thresholding to get binary image
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 19, 10);

        // Invert the binary image
        Core.bitwise_not(src, src);

        // Detect lines using Hough transform
        Imgproc.HoughLinesP(src, lines, 1, Math.PI / 180, 120, 170, 60);

        // Draw red lines over detected lines to remove them
        for (int i = 0; i < lines.rows(); i++) {
            final double[] lineInfo = lines.get(i, 0);

            Imgproc.line(src,
                    new Point(lineInfo[0], lineInfo[1]), new Point(lineInfo[2], lineInfo[3]),
                    new Scalar(0, 0, 255),
                    4 // You can adjust the thickness of the line if needed
            );
        }

        // Convert the image back to BGR
        Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2BGR);

        // Release resources
        lines.release();
    }

    // Output each cell of the Sudoku grid as individual images
    static void outputCells(Mat src) {
        int cellWidth = src.width() / 9; // Calculate the width of each cell
        int cellHeight = src.height() / 9; // Calculate the height of each cell
        Size cellSize = new Size(cellWidth, cellHeight); // Define the size of each cell

        // Loop through each cell in the 9x9 Sudoku grid
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                double xPos = cellWidth * col; // X position of the cell
                double yPos = cellHeight * row; // Y position of the cell

                Rect cellRect = new Rect(new Point(xPos, yPos), cellSize); // Define the rectangle for the cell

                Mat cell = new Mat(src, cellRect); // Extract the cell from the source image

                int cellCount = row * 9 + col + 1; // Calculate the cell number

                // Save the cell image to a file
                Imgcodecs.imwrite("data/cells/cell_" + cellCount + ".png", cell);
            }
        }
    }
}

// Class to call the "main" class
public class HoughLines {

    public static void main(Mat frame, String... args) {

        // Load native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Run main class
        HoughLinesRun.run(frame);
    }
}
