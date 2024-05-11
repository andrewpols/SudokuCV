// This program implements a video capture that reads successive frames,
// then uses Computer Vision to read a Sudoku grid, process the image and its information,
// and then applies a solving algorithm that outputs the keystrokes to solve the puzzle online.
// Andrew Pols - ICS4U; Last Update: May 7, 2024

// The "Main" Class.
// All classes run through this class.
public class SudokuCV {

    public static void main(String[] args) throws InterruptedException {

        SudokuStream.main(args); // Run video capture

        Thread.sleep(6000); // Give the program time to set up the video capture

        // This while block runs while the video is still opened, preventing the code from moving on to the next step
        while (SudokuStream.isVideoOpened()) {

        }

        // Allow the previous threads to close safely by letting the program sleep
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        // Run image processing on the frame captured from the stream
        HoughLines.main(args);

        // This while block prevents the next step if the 81 cells have not yet been written into the data directory.
        while (!HoughLines.areCellsExtracted()) {

        }

        // Calls the sudoku solving algorithm
        SudokuSolver.main(args);

    }
}
