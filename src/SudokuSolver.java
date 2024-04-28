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

    static int[][] sudokuArray = new int[9][9];
    static int[][] sudokuCopy = new int[9][9];

    public void getCellOCR() {

        ArrayList<String> cellData = new ArrayList<>();

        Tesseract tesseract = new Tesseract();


        for (int i=1; i<=81; i++) {
            try {
                if (i==1) {
                    tesseract.setDatapath("/usr/local/Cellar/tesseract/5.3.4_1/share/tessdata");
                }

                String output = tesseract.doOCR(new File("data/Cell_" + i + ".jpg"));

                if (output.isEmpty()) {
                    output = "0";
                }



                output = String.valueOf(output.charAt(0));

                cellData.add(output);


            }
            catch (TesseractException e) {
                e.printStackTrace();
            }
        }

        Collections.reverse(cellData);

        System.out.println(cellData);

        // Convert into 2D Array
        int rowCounter = 0;
        int columnCounter = 0;

        Iterator<String> iter = cellData.iterator();

        while(rowCounter < 9) {
            sudokuArray[rowCounter][columnCounter] = Integer.parseInt(iter.next());
            columnCounter++;

            if (columnCounter == 9) {
                columnCounter = 0;
                rowCounter++;
            }

        }

        sudokuCopy = sudokuArray.clone();
        System.out.println("Copy: " + Arrays.deepToString(sudokuCopy));
    }

    // TODO: make a clone of the original, if it starts with 0, we don't input a key.
    // Else, add the number in the final sudokuArray


    public Boolean isPossible(int row, int column, int possibleNumber) {

        // Check row of number
        for (int i = 0; i < sudokuArray.length; i++) {
            if (sudokuArray[row][i] == possibleNumber) {
                return false;
            }
        }

        // Check Column of number
        for (int i = 0; i < sudokuArray.length; i++) {
            if (sudokuArray[i][column] == possibleNumber) {
                return false;
            }
        }

        // Check 3x3 square
        int columnOrigin = Math.floorDiv(column, 3) * 3;
        int rowOrigin = Math.floorDiv(row, 3) * 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (sudokuArray[rowOrigin + i][columnOrigin + j] == possibleNumber) {
                    return false;
                }
            }
        }

        return true;
    }

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
    }

    public void typeSudokuAnswer() {
        try {
            Robot robot = new Robot();

            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            System.out.println(Arrays.deepToString(sudokuCopy));


            for (int i = 0; i < sudokuArray.length; i++) {
                for (int j = 0; j < sudokuArray[i].length - 1; j++) {
                    if (sudokuCopy[i][j] == 0) {
                        int currentInput = sudokuArray[i][j];
                        robot.keyPress(48 + currentInput);

                    }

                    robot.keyPress(KeyEvent.VK_RIGHT);

                    if (j==7) {
                          robot.keyPress(48 + sudokuArray[i][j+1]);
                          if (i!=8) {
                              robot.keyPress(KeyEvent.VK_DOWN);
                              for (int k=0; k<8; k++) {
                                  robot.keyPress(KeyEvent.VK_LEFT);

                              }
                          }
                    }
                }
            }
        } catch (AWTException e) {
            e.getStackTrace();
        }

    }


    public static void main(String[] args) {

        System.out.println("\n" + Arrays.deepToString(sudokuArray) + "\n");

        SudokuSolver sudoku = new SudokuSolver();

        sudoku.getCellOCR();

        sudoku.answerPuzzle(sudokuArray);

        System.out.println("\n" + Arrays.deepToString(sudokuArray) + "\n");

        sudoku.typeSudokuAnswer();


    } // end of main

} // end of class
