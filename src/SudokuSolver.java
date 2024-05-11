import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class SudokuSolver {

    // Initialize arrays to perform the solving algorithm
    static int[][] sudokuArray = new int[9][9];

    // A copy of the array is used to recognize where the predefined numbers exist
    // to avoid typing that item out after solving
    static int[][] sudokuCopy = new int[9][9];

    // This method takes the extracted cell images in the data directory
    // and performs an OCR with Tess4J (Tesseract for Java), placing the read values into the Sudoku array
    public void getCellOCR() {

        ArrayList<String> cellData = new ArrayList<>(); // Initialize and assign list of data to be read

        Tesseract tesseract = new Tesseract(); // Instantiate tess obj

        // Loop through the 81 cell images
        for (int i=1; i<=81; i++) {
            try {
                if (i==1) {

                    // Tesseract must be directed to the path containing the tessdata to properly operate
                    tesseract.setDatapath("/usr/local/Cellar/tesseract/5.3.4_1/share/tessdata");
                }

                System.out.println("\n On Image Number: " + i); // For terminal update purposes

                // This is the actual OCR command for Tesseract
                // Each cell follows the name: "Cell_number.jpg", where number is denoted by i
                String output = tesseract.doOCR(new File("data/Cell_" + i + ".jpg"));

                // If tesseract does not read anything in the cell, that is represented by a zero in the array
                if (output.isEmpty()) {
                    output = "0";
                }

                // Tesseract sometimes adds unnecessary spaces or characters following the read character
                // To offset this, only consider the first character as the output
                output = String.valueOf(output.charAt(0));

                // Add the read String into the list
                cellData.add(output);

            }
            catch (TesseractException e) {
                e.printStackTrace();
            }
        }

        // Tesseract reads in reverse order, so we reverse the cell data list
        Collections.reverse(cellData);

        System.out.println(cellData); // debugging purposes

        // Convert into 2D Array
        // rowCounter marks the row we are on, and will increment every 9th column
        int rowCounter = 0;
        // columnCounter marks the column we are on, and increments every iteration,
        // but resets to 0 after every 9th column
        int columnCounter = 0;

        Iterator<String> iter = cellData.iterator();

        while(rowCounter < 9) {
            sudokuArray[rowCounter][columnCounter] = Integer.parseInt(iter.next()); // set array item to the parsed int
            columnCounter++; // move to next column

            // If we're on the last column (9th), move on to the next row and reset columnCounter to 0
            if (columnCounter == 9) {
                columnCounter = 0;
                rowCounter++;
            }

        }

        sudokuCopy = sudokuArray.clone(); // Now with the input data, clone the array mentioned earlier

        System.out.println("Copy: " + Arrays.deepToString(sudokuCopy));
    }

    // This method takes a possible number (taken from the answerPuzzle method) and checks if its valid within the grid
    // eg. For possibleNumber = 2, is there a 2 in the row, column, or 3x3 grid in which we are checking?
    // So, we return true if possible and false if not possible
    public Boolean isPossible(int row, int column, int possibleNumber) {

        // Check the row of the possible number
        for (int i = 0; i < sudokuArray.length; i++) {
            // If the same number exists in the row, we violate the rules and so return false
            if (sudokuArray[row][i] == possibleNumber) {
                return false;
            }
        }

        // If the same number exists in the column, we violate the rules and so return false
        for (int i = 0; i < sudokuArray[i].length; i++) {
            if (sudokuArray[i][column] == possibleNumber) {
                return false;
            }
        }

        // Algorithm for checking the 3x3 grid
        // In each grid, the starting row/column will be in the form of 3k, for some integer k
        // Thus, each starting row/column is divisible by 3. To get to this starting column,
        // we divide our current column by 3, ROUND DOWN, and then multiply by 3, getting us to the nearest multiple of 3
        // eg. currColumn = 5; currColumn floor div by 3 =  5 / 3 = 1.66667 = 1 * 3 = 3 (colOrigin = 3)
        int columnOrigin = Math.floorDiv(column, 3) * 3;
        int rowOrigin = Math.floorDiv(row, 3) * 3;

        // Loop through the starting col/row, PLUS the 2 cols/rows following the origin
        // This allows us to loop through the 3x3 grid
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // If the same number exists in the 3x3 grid, we violate the rules and so return false
                if (sudokuArray[rowOrigin + i][columnOrigin + j] == possibleNumber) {
                    return false;
                }
            }
        }

        // If we've passed all the previous conditions, we have not violated the rules and so return true
        return true;
    } // end isPossible() method


    // This method is what actually comes up with the solution for the grid.
    // We start in the 0,0 index (top left) and perform the following check in the method.

    /*
    If it passes the isPossible() conditions, we continue and call the function recursively.
    For a recursive method, we take the updated SudokuArray and call the function again.
    If we keep passing the isPossible() conditions, we continue calling recursively until the board is filled and we return true
    If at any point we fail the isPossible() conditions, we return false on the last recursive call,
    return back to the previous call, update the row-col to 0, and try again with a new number
    */
    public boolean answerPuzzle(int[][] sudokuArray) {
        for (int row = 0; row < sudokuArray.length; row++) {
            for (int column = 0; column < sudokuArray.length; column++) {
                if (sudokuArray[row][column] == 0) {
                    for (int possibleNumber = 1; possibleNumber < sudokuArray.length + 1; possibleNumber++) {
                        if (isPossible(row, column, possibleNumber)) {

                            sudokuArray[row][column] = possibleNumber;

                            if (answerPuzzle(sudokuArray)) {
                                return true;
                            } else {
                                sudokuArray[row][column] = 0;
                            }
                        }
                    }
                    return false;
                }
            }
        }

        return true;
    } // end of answerPuzzle() method


    // This method takes the fully updated sudokuArray and simulates the key strokes needed to type out
    // the answer on a web-based Sudoku puzzle
    public void typeSudokuAnswer() {
        try {
            Robot robot = new Robot(); // instantiate Robot obj needed to access keyboard

            // After the code starts running, the cursor will take priority in the terminal.
            // To work around this, we press the mouse on the cell we were in to get back to the website
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);

            System.out.println(Arrays.deepToString(sudokuCopy)); // print for debugging purposes


            // Simple loop through the 2D sudokuArray
            for (int i = 0; i < sudokuArray.length; i++) {

                // We will loop through all the columns except the last one, which will have its own set of commands
                for (int j = 0; j < sudokuArray[i].length - 1; j++) {
                    if (sudokuCopy[i][j] == 0) { // if empty, we update (this is where sudokuCopy is useful)

                        // get the current number for the row-col in sudokuArray
                        int currentInput = sudokuArray[i][j];

                        // 48 is the keycode for zero in Robot. Subsequent numbers will follow 48.
                        // eg. 48 + 2 = "2"
                        robot.keyPress(48 + currentInput); // press key for currNum

                    }

                    robot.keyPress(KeyEvent.VK_RIGHT); // move right to next cell

                    // Proceed if we are on the second last column
                    if (j==7) {
                          robot.keyPress(48 + sudokuArray[i][j+1]); // Type the last number of the col
                          // Only proceed if not on the last row (otherwise, on some websites, this sends us back to Cell 1)
                          if (i!=8) {
                              robot.keyPress(KeyEvent.VK_DOWN); // Move down a row
                              for (int k=0; k<8; k++) {
                                  robot.keyPress(KeyEvent.VK_LEFT); // Move left 8 times to get back to col 1 of the next row

                              }
                          }
                    }
                }
            }
        } catch (AWTException e) {
            e.getStackTrace();
        }

    } // end of typeSudokuAnswer() method


    public static void main(String[] args) {

        System.out.println("\n" + Arrays.deepToString(sudokuArray) + "\n"); // debugging purposes

        SudokuSolver sudoku = new SudokuSolver(); // instantiate sudoku obj

        // Perform relevant methods
        sudoku.getCellOCR();

        sudoku.answerPuzzle(sudokuArray);

        System.out.println("\n" + Arrays.deepToString(sudokuArray) + "\n");

        sudoku.typeSudokuAnswer();


    } // end of main

} // end of class
