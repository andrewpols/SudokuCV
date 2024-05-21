// This file takes the extracted cells from HoughLines and runs an OCR to attain
// an array of the Sudoku grid. This array is used in an algorithm that solves the puzzle
// and returns the solution on an image at the end.

// Import necessary libraries
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SudokuSolver {

    // Initialize the main array for the Sudoku grid
    static int[][] sudokuArray = new int[9][9];
    static boolean isPuzzleSolved = true;

    // A copy of the Sudoku grid to keep track of predefined numbers
    static int[][] sudokuCopy = new int[9][9];

    // This method performs OCR on the cell images and fills the Sudoku grid
    public static void getCellOCR() {
        ArrayList<String> cellData = new ArrayList<>(); // List to store OCR results for each cell
        Tesseract tesseract = new Tesseract(); // Instantiate Tesseract object

        // Loop through the 81 cell images
        for (int i = 1; i <= 81; i++) {
            try {
                if (i == 1) {
                    // Set the path to tessdata and specify the language for OCR
                    tesseract.setDatapath("/Users/andrewpols/Downloads/tessdata_shreetest-master");
                    tesseract.setLanguage("digits");
                }

                // Perform OCR on the cell image
                String output = tesseract.doOCR(new File("data/cells/cell_" + i + ".png"));

                // Remove all non-digit characters from the OCR result
                output = output.replaceAll("[^0-9]", "");

                // If OCR result is empty, set it to "0"
                if (output.isEmpty()) {
                    output = "0";
                }

                // Consider only the first character if OCR result has multiple characters
                output = String.valueOf(output.charAt(0));

                // Add the processed OCR result to the list
                cellData.add(output);

            } catch (TesseractException e) {
                e.printStackTrace();
            }
        }

        System.out.println("1D Array Data: " + cellData); // Print the OCR results for debugging

        // Convert the list of OCR results to a 2D array
        int rowCounter = 0;
        int columnCounter = 0;

        Iterator<String> iter = cellData.iterator();
        while (rowCounter < 9) {
            sudokuArray[rowCounter][columnCounter] = Integer.parseInt(iter.next()); // Fill the main array
            sudokuCopy[rowCounter][columnCounter] = sudokuArray[rowCounter][columnCounter]; // Fill the copy array
            columnCounter++;

            // Move to the next row after every 9 columns
            if (columnCounter == 9) {
                columnCounter = 0;
                rowCounter++;
            }
        }

        System.out.println("2D Array Data: " + Arrays.deepToString(sudokuArray)); // Print the copy array for debugging
    }

    // Method to check if placing a number at a given position is valid
    public Boolean isPossible(int row, int column, int possibleNumber) {

        // Check if the number already exists in the current row
        for (int i = 0; i < sudokuArray.length; i++) {
            if (sudokuArray[row][i] == possibleNumber) {
                return false;
            }
        }

        // Check if the number already exists in the current column
        for (int i = 0; i < sudokuArray.length; i++) {
            if (sudokuArray[i][column] == possibleNumber) {
                return false;
            }
        }

        // Determine the starting indices of the 3x3 sub-grid
        int columnOrigin = Math.floorDiv(column, 3) * 3;
        int rowOrigin = Math.floorDiv(row, 3) * 3;

        // Check if the number already exists in the 3x3 sub-grid
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (sudokuArray[rowOrigin + i][columnOrigin + j] == possibleNumber) {
                    return false;
                }
            }
        }

        // If the number doesn't exist in the row, column, or 3x3 sub-grid, it is valid
        return true;
    } // end isPossible() method

    // Recursive method to solve the Sudoku puzzle using backtracking
    public boolean answerPuzzle(int[][] sudokuArray) {
        // Loop through rows and columns
        for (int row = 0; row < sudokuArray.length; row++) {
            for (int column = 0; column < sudokuArray.length; column++) {
                if (sudokuArray[row][column] == 0) { // Check for empty cells
                    // Check possibilities of numbers recursively, starting at one
                    for (int possibleNumber = 1; possibleNumber <= 9; possibleNumber++) {
                        if (isPossible(row, column, possibleNumber)) {
                            sudokuArray[row][column] = possibleNumber; // Place the number

                            // Recursively attempt to fill in the rest of the grid
                            if (answerPuzzle(sudokuArray)) {
                                return true;
                            } else {
                                sudokuArray[row][column] = 0; // Reset the cell if it leads to no solution
                            }
                        }
                    }
                    return false; // Return false if no valid number is found for the current cell
                }
            }
        }
        return true; // Return true if the entire grid is filled without conflict
    } // end of answerPuzzle() method

    // This method draws the solution numbers on the Sudoku grid image
    public static Mat drawSolutions(Mat src) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Get cell dimensions (it's an even 9x9 grid, so each cell will have 1/9 of the image's height & width)
        int cellWidth = src.width() / 9;
        int cellHeight = src.height() / 9;

        // Loop through the 2D sudokuCopy array
        for (int i = 0; i < sudokuCopy.length; i++) {
            for (int j = 0; j < sudokuCopy[i].length; j++) {
                if (sudokuCopy[i][j] == 0) { // Check if the cell was initially empty
                    // Experimental values of positions that worked for all tested puzzles
                    double xPos = cellWidth * j + cellWidth * 0.25;
                    double yPos = cellHeight * i + cellHeight * 0.80;

                    int currentInput = sudokuArray[i][j]; // Get the solved number for the cell

                    // Draw the solved number on the image
                    Imgproc.putText(src, String.valueOf(currentInput), new Point(xPos, yPos), 4, 1.5,
                            new Scalar(255, 127, 100), 2);

                    if (sudokuArray[i][j] == 0) {
                        isPuzzleSolved = false;
                    }
                }
            }
        }

        // Save the solved Sudoku image
        Imgcodecs.imwrite("data/images/solved.png", src);

        return src;
    } // end of drawSolutions() method

    public static void main(String... args) {

        SudokuSolver sudoku = new SudokuSolver(); // Instantiate SudokuSolver object

        // Perform relevant methods
        getCellOCR();

        sudoku.answerPuzzle(sudokuArray); // Solve the Sudoku puzzle

        // Check if the puzzle is solvable
        if (Arrays.stream(sudokuArray).anyMatch(row -> Arrays.stream(row).anyMatch(cell -> cell == 0))) {
            System.out.println("Not solvable");
            return;
        }

        System.out.println("\n Solved Sudoku Array:\n" + Arrays.deepToString(sudokuArray)); // Print solved Sudoku array

    } // end of main method

} // end of SudokuSolver class
