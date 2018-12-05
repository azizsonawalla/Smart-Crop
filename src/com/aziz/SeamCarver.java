package com.aziz;

import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class SeamCarver {

    /* 2D array with shortest paths */
    private ArrayList<ArrayList<Pair<Integer, Integer>>> BACKTRACKING_MATRIX;
    /* Image file to seam carve */
    private BufferedImage INPUT_IMAGE;
    /* 2D array with RGB values of input image */
    private ArrayList<ArrayList<Pixel>> PIXEL_ARRAY;
    private int PIXEL_ARRAY_HEIGHT;
    private int PIXEL_ARRAY_WIDTH;


    public SeamCarver(String imagePathIn) {
        // load image from file
        File imageFile = new File(imagePathIn);
        try {
            INPUT_IMAGE = ImageIO.read(imageFile);
        } catch (Exception e) {
            System.err.print(e.getMessage());
            return;
        }

        // convert input image to array of Pixels
        PIXEL_ARRAY = new ArrayList<ArrayList<Pixel>>();
        for (int y = 0; y < INPUT_IMAGE.getHeight(); y++) {
            ArrayList<Pixel> row = new ArrayList<>();
            for (int x = 0; x < INPUT_IMAGE.getWidth(); x++) {
                row.add(new Pixel(INPUT_IMAGE.getRGB(x, y), x, y));
            }
            PIXEL_ARRAY.add(row);
        }
    }

    public void carve(int cols, String imagePathOut) {
        for (int col = 0; col < cols; col++) {
            // initialize PIXEL_ARRAY dimensions
            PIXEL_ARRAY_HEIGHT = PIXEL_ARRAY.size();
            PIXEL_ARRAY_WIDTH = PIXEL_ARRAY.get(0).size();

            // initialize Pixel energies
            for (int y = 0; y < PIXEL_ARRAY_HEIGHT; y++) {
                for (int x = 0; x < PIXEL_ARRAY_WIDTH; x++) {
                    Pixel currentPixel = PIXEL_ARRAY.get(y).get(x);
                    currentPixel.setEnergy(energyValueOf(x, y));
                }
            }

            // initialize Pixel cumulative energies and backtracking matrix
            BACKTRACKING_MATRIX = new ArrayList<>();
            for (int y = 0; y < PIXEL_ARRAY_HEIGHT; y++) {
                ArrayList<Pair<Integer, Integer>> backtracking_row = new ArrayList<Pair<Integer, Integer>>();
                for (int x = 0; x < PIXEL_ARRAY_WIDTH; x++) {
                    Pixel currentPixel = PIXEL_ARRAY.get(y).get(x);
                    if (y == 0) {
                        currentPixel.setCumulativeEnergy(currentPixel.getEnergy());
                        backtracking_row.add(currentPixel.getPos());
                    } else {
                        Pixel prevPixel = minimumPixel(currentPixel);
                        currentPixel.setCumulativeEnergy(currentPixel.getEnergy() + prevPixel.getCumulativeEnergy());
                        backtracking_row.add(prevPixel.getPos());
                    }
                }
                BACKTRACKING_MATRIX.add(backtracking_row);
            }

            //        String colors = "";
            //        String energies = "";
            //        String cumulative = "";
            //        for (int y = 0; y < PIXEL_ARRAY_HEIGHT; y++) {
            //            for (int x = 0; x < PIXEL_ARRAY_WIDTH; x++) {
            //                Pixel currentPixel = PIXEL_ARRAY.get(y).get(x);
            //                energies = energies.concat(Double.toString(currentPixel.getEnergy()).concat("  "));
            //                cumulative = cumulative.concat(Double.toString(currentPixel.getCumulativeEnergy()).concat("  "));
            //                colors = colors.concat(Integer.toString(currentPixel.getRed())).concat(",").concat(Integer.toString(currentPixel.getGreen())).concat(",").concat(Integer.toString(currentPixel.getBlue())).concat("  ");
            //            }
            //            energies = energies.concat("\n");
            //            colors = colors.concat("\n");
            //            cumulative = cumulative.concat("\n");
            //        }
            //
            //        System.out.print(colors);
            //        System.out.print(energies);
            //        System.out.print(cumulative);


            // remove least energy rows
            ArrayList<Pair<Integer, Integer>> path = leastEnergyVerticalPath();
            PIXEL_ARRAY = removeElements(path, PIXEL_ARRAY);
        }

        // create carved image
        int carvedImageHeight = PIXEL_ARRAY_HEIGHT;
        int carvedImageWidth = PIXEL_ARRAY_WIDTH - cols;
        BufferedImage carvedImage = new BufferedImage(carvedImageWidth, carvedImageHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < carvedImageHeight; y++) {
            for (int x = 0; x < carvedImageWidth; x++) {
                carvedImage.setRGB(x, y, PIXEL_ARRAY.get(y).get(x).getRGB());
            }
        }

        // write carvedImage back to file
        try {
            ImageIO.write(carvedImage, "png", new File(imagePathOut));
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }

    private Pixel minimumPixel(Pixel currentPixel) {
        Pixel Left = PIXEL_ARRAY.get(currentPixel.getY() - 1).get(mod(currentPixel.getX() - 1, PIXEL_ARRAY_WIDTH));
        Pixel Center = PIXEL_ARRAY.get(currentPixel.getY() - 1).get(currentPixel.getX());
        Pixel Right = PIXEL_ARRAY.get(currentPixel.getY() - 1).get(mod(currentPixel.getX() + 1, PIXEL_ARRAY_WIDTH));

        double minimumCE = Math.min(Math.min(Left.getCumulativeEnergy(), Center.getCumulativeEnergy()), Right.getCumulativeEnergy());
        if (minimumCE == Left.getCumulativeEnergy()) {
            return Left;
        }
        if (minimumCE == Center.getCumulativeEnergy()) {
            return Center;
        }
        return Right;
    }

    private ArrayList<ArrayList<Pixel>> removeElements(ArrayList<Pair<Integer, Integer>> path, ArrayList<ArrayList<Pixel>> array) {
        for (int i = 0; i < path.size(); i++) {
            Pair<Integer, Integer> pair = path.get(i);
            int x = pair.getKey();
            int y = pair.getValue();
            array.get(y).remove(x);
        }
        return array;
    }

    private double energyValueOf(int x, int y) {
        /* Using dual gradient energy function */

        // calculate neighbours
        Pixel xPrev = PIXEL_ARRAY.get(y).get(mod(x - 1, PIXEL_ARRAY_WIDTH));
        Pixel xNext = PIXEL_ARRAY.get(y).get(mod(x + 1, PIXEL_ARRAY_WIDTH));
        Pixel yPrev = PIXEL_ARRAY.get(mod(y - 1, PIXEL_ARRAY_HEIGHT)).get(x);
        Pixel yNext = PIXEL_ARRAY.get(mod(y + 1, PIXEL_ARRAY_HEIGHT)).get(x);

        // calculate RGB gradients in x direction
        double deltaX_Red = Math.abs(xPrev.getRed() - xNext.getRed());
        //System.out.print("\nDeltaX Red: ".concat(Double.toString(deltaX_Red)));
        double deltaX_Green = Math.abs(xPrev.getGreen() - xNext.getGreen());
        //System.out.print("\nDeltaX Green: ".concat(Double.toString(deltaX_Green)));
        double deltaX_Blue = Math.abs(xPrev.getBlue() - xNext.getBlue());
        //System.out.print("\nDeltaX Blue: ".concat(Double.toString(deltaX_Blue)));

        // calculate RGB gradients in y direction
        double deltaY_Red = Math.abs(yPrev.getRed() - yNext.getRed());
        //System.out.print("\nDeltaY Red: ".concat(Double.toString(deltaY_Red)));
        double deltaY_Green = Math.abs(yPrev.getGreen() - yNext.getGreen());
        //System.out.print("\nDeltaY Green: ".concat(Double.toString(deltaY_Green)));
        double deltaY_Blue = Math.abs(yPrev.getBlue() - yNext.getBlue());
        //System.out.print("\nDeltaY Blue: ".concat(Double.toString(deltaY_Blue)));

        // calculate x and y gradients
        double deltaX = Math.pow(deltaX_Red, 2) + Math.pow(deltaX_Green, 2) + Math.pow(deltaX_Blue, 2);
        double deltaY = Math.pow(deltaY_Red, 2) + Math.pow(deltaY_Green, 2) + Math.pow(deltaY_Blue, 2);

        return deltaX + deltaY;
    }

    public int mod(int num, int modulo) {
        if (num >= 0 && num < modulo) {
            return num;
        }
        if (num >= 0) {
            return num % modulo;
        }
        return modulo + num;
    }

    private ArrayList<Pair<Integer, Integer>> leastEnergyVerticalPath() {
        ArrayList<Pixel> lastRow = PIXEL_ARRAY.get(PIXEL_ARRAY_HEIGHT - 1);
        Pixel minimumCEPixel = lastRow.get(0);

        for (int i = 1; i < lastRow.size(); i++) {
            Pixel currPixel = lastRow.get(i);
            if (currPixel.getCumulativeEnergy() < minimumCEPixel.getCumulativeEnergy()) {
                minimumCEPixel = currPixel;
            }
        }

        Pair<Integer, Integer> currentPos = minimumCEPixel.getPos(); // start point of path
        Pair<Integer, Integer> nextPos = BACKTRACKING_MATRIX.get(currentPos.getValue()).get(currentPos.getKey());
        ArrayList<Pair<Integer, Integer>> path = new ArrayList<Pair<Integer, Integer>>();

        while (currentPos != nextPos) {
            path.add(currentPos);
            currentPos = nextPos;
            nextPos = BACKTRACKING_MATRIX.get(currentPos.getValue()).get(currentPos.getKey());
        }

        return path;
    }

    public static void main(String[] args) {
        SeamCarver carver = new SeamCarver("sample-images/sample1-0.png");
        carver.carve(50, "sample-images/sample1-1.png");
    }
}
