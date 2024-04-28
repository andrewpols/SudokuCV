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
class HoughLinesRun {
    public void run(String[] args) {

//        VideoCapture videoCapture = new VideoCapture();
//        videoCapture.open(1);
//
//        Mat vFrame = new Mat();
//        JPanel panel = new JPanel();
//
//        while (videoCapture.isOpened()) {
//            if (videoCapture.read(vFrame)) {
//                Imgcodecs.imwrite("test.jpg", vFrame);
//                HighGui.createJFrame("Video", 0);
//
//            }
//        }


        // Declare output variables
        Mat dst = new Mat(), cdst = new Mat();

        String defaultFile = "sudoku_test.png";
        String filename = ((args.length > 0) ? args[0] : defaultFile);

        // Load image
        Mat src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE);

        // Check if image is loaded properly
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
        Imgproc.HoughLines(dst, lines, 1, Math.PI / 180, 150); // runs the actual detection

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


        // Show Results
        HighGui.imshow("Source", src);
        HighGui.imshow("Binary Image Transform", cdst);
        Imgcodecs.imwrite("grayscale.jpg", cdst);


        extractCellsFromImage(cdst, src);


        // Wait and Exit
        HighGui.waitKey();
        System.exit(0);

    }

    private static void extractCellsFromImage(Mat cdst, Mat src) {

        ArrayList<Mat> cells = new ArrayList<>();
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> cellContours = new ArrayList<>();
        Imgproc.threshold(src, cdst, 250, 400, Imgproc.THRESH_BINARY);
        Imgproc.findContours(cdst, cellContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgcodecs.imwrite("binary.jpg", cdst);


        for (MatOfPoint contour : cellContours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.area() > 10) {
                Mat cell = new Mat(cdst, rect);
                cells.add(cell);

            }

        }

        int cellNumber = 1;

        for (Mat cell : cells) {
            Imgcodecs.imwrite("data/Cell_" + cellNumber + ".jpg", cell);

            cellNumber++;
        }
    }
}


public class HoughLines {

    public static void main(String[] args) {

        // Load native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        new HoughLinesRun().run(args);

    }
}
