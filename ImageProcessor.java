// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP112 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP 112 Assignment
 * Name: David Dobbie
 * Usercode: 300340161
 * ID: dobbiedavi
 */

import ecs100.*;
import java.util.*;
import java.io.*;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.JColorChooser;

/** ImageProcessor allows the user to load, display, modify, and save an image in a number of ways.
The program should include
- Load, commit, save. (Core)
- Brightness adjustment (Core)
- Horizontal flip and 90 degree rotation. (Core)
- Merge  (Core)
- Crop&Zoom  (Core)
- Blur (3x3 filter)  (Core)

- Rotate arbitrary angle (Completion)
- Pour (spread-fill)  (Completion)
- General Convolution Filter  (Completion)

- Red-eye detection and removal (Challenge)
- Filter brush (Challenge)
 */
public class ImageProcessor {
    private Color image[][];
    private Color editedImage[][];
    private Color mergeImage[][] = null;
    private Double selectRect[] = new Double[] {0.0,0.0,0.0,0.0}; //first two are x,y for one corner, next are x,y for opposite corner
    private boolean drawnOver = false;
    private boolean toPour = false;
    private boolean toFilterBrush = false;
    private boolean toSelect = false;
    private int pourThreshold = 5;
    private Color paint;
    private Double convoFilter[][] = null;

    /**
     * Constructs the ImageProcessor class, making all the buttons and sliders that will
     * manipulate the image in a series of ways. Also it will set up a mouse listener for
     * some of the actions that will be done.
     */
    public ImageProcessor(){
        UI.initialise();
        UI.addButton("Load", this::load);
        UI.addButton("Save", this::saveImage);
        UI.addButton("Commit", this::commit);
        UI.addSlider("Brightness", -100, 100, 0, this::brightness);
        UI.addButton("Horizontal Flip", this::horizontalFlip);
        UI.addButton("Rotate 90 Degrees Left", this::rotateLeft90);
        UI.addButton("Add File to Merge", this::chooseMerge);
        UI.addSlider("Merge Opacity", 0, 100, 50, this::merge);
        UI.addButton("Crop & Zoom", this::cropZoom);
        UI.addButton("Blur", this::blur);
        UI.addSlider("Rotate", -180, 180, 0, this::rotate);
        UI.addButton("Set Paint", this::setPaint);
        UI.addButton("Pour", this::pour);
        UI.addSlider("Set Threshold", 0, 10, 5, this::setThreshold);
        UI.addButton("Load Convolution Filter", this::loadConvo);
        UI.addButton("Use Convolution Filter", this::useConvo);
        UI.addButton("Use Select Tool", this::useSelectTool);
        UI.addButton("Filter Brush", this::enableFilterBrush);
        UI.setMouseMotionListener(this::doMouse);
    }

    /**
     * starts the load process, assigns the image color array to the one obtained by loading
     */
    public void load(){
        image = loadImage();
        if (image == null){
            UI.println("No image loaded");
        }else{
            editedImage = copyImage(image);
            draw();
        }
    }

    /**
     * copies the values of the image and returns those values so the image copying it doesn't
     * interact directly with the 
     */
    public Color[][] copyImage(Color[][] img){
        int rows = img.length;
        int cols = img[0].length;
        Color[][] ans = new Color[rows][cols];
        for (int row = 0; row < rows; row++){
            for(int col = 0; col < cols; col ++){
                Color c = img[row][col];
                ans[row][col] = c;
            }
        }
        return ans;
    }

    /**
     * loads the image onto the color array, a format that will allow it to be manipulated and changed.
     */
    public Color[][] loadImage(){
        String imageName = UIFileChooser.open();
        if (imageName == null) {return null;}
        try {
            BufferedImage img = ImageIO.read(new File(imageName));
            int rows = img.getHeight();
            int cols = img.getWidth();
            Color[][] ans = new Color[rows][cols];
            for (int row = 0; row < rows; row++){
                for(int col = 0; col < cols; col++){
                    Color c = new Color(img.getRGB(col, row));
                    ans[row][col] = c;
                }
            }
            UI.printMessage("Loaded " + imageName);
            return ans;
        } catch(IOException e){UI.println("Image reading failed: "+e);}
        return null;
    }

    /**
     * save the image into an image format, converting the colour array into an image file
     */
    public void saveImage() {
        int rows = image.length;
        int cols = image[0].length;
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < rows; row++){
            for (int col = 0; col < cols; col++){
                Color c = this.image[row][col];
                img.setRGB(col, row, c.getRGB());
            }
        }
        try{
            String fname = UIFileChooser.save("save to png image file");
            if (fname==null) return;
            File imageFile = new File(fname);
            ImageIO.write(img, "png", new File(fname));
        } catch(IOException e){UI.println("Image reading failed "+e);}
    }

    /**
     * draws the image and editedImage on the screen
     */
    public void draw(){
        //draw orignal image
        drawImage();
        //draw edited image
        drawEditedImage();
    }

    /**
     * Draw the orginal image that is loaded up.
     */
    public void drawImage(){
        UI.eraseRect(editedImage[0].length, 0, image[0].length, image.length);
        for (int row = 0; row < image.length; row ++){
            for (int col = 0; col< image[row].length; col ++){
                UI.setColor(image[row][col]);
                UI.fillRect(col + editedImage[0].length,row, 1, 1);
            } 
        }
    }

    /**
     * redraw the editedImage, the one that is being manipulated by the programm for feedback to the user
     */
    public void drawEditedImage(){
        UI.eraseRect(0, 0, editedImage.length, editedImage[0].length);
        for (int row = 0; row < editedImage.length; row ++){
            for (int col = 0; col< editedImage[row].length; col ++){
                UI.setColor(editedImage[row][col]);
                UI.fillRect(col,row, 1, 1);
            } 
        } 
        drawnOver = true;
    }

    /**
     * changes the brightness of the editedImage. It uses values from the image to make these calculations
     */
    public void brightness(double value){
        double stepValue = value * 2.55;
        int r;
        int g;
        int b;
        for (int row = 0; row < editedImage.length; row ++){
            for (int col = 0; col < editedImage[row].length; col ++){
                //set r g b values
                r = (int)(image[row][col].getRed() + stepValue);
                g = (int)(image[row][col].getGreen() + stepValue);
                b = (int)(image[row][col].getBlue() + stepValue);
                //check if over/under limits
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                Color c = new Color(r, g, b);
                editedImage[row][col] = c;
            } 
        } 
        drawEditedImage();
    }

    /**
     * Flip the image horizontally by making a temporary image that is manipulated
     * using the editedImge values
     */
    public void horizontalFlip(){
        Color imageTemp[][] = new Color[editedImage.length][editedImage[0].length];
        for (int row = 0; row < imageTemp.length; row ++){
            for (int col = 0; col < imageTemp[row].length; col ++){
                Color c = editedImage[row][col];
                imageTemp[row][imageTemp[row].length - 1 - col] = c;
            }
        }
        editedImage = copyImage(imageTemp);
        drawEditedImage();
    }

    /**
     * Rotates the image left by 90 degrees
     */
    public void rotateLeft90(){
        Color imageTemp[][] = new Color[editedImage[0].length][editedImage.length];
        for (int row = 0; row < editedImage.length; row ++){
            for (int col = 0; col < editedImage[row].length; col ++){
                Color c = editedImage[row][col];
                imageTemp[editedImage[0].length - 1 - col][row] = c;
            }
        }
        editedImage = copyImage(imageTemp);
        drawEditedImage();
        drawImage();
    }

    /**
     * Merge this image with another image chosen
     */
    public void chooseMerge(){
        mergeImage = loadImage();
        merge(50); //start in middle of the merge aspect
    }

    /**
     * allows the user to control the intensity of the opacity for the merging of the images together
     */
    public void merge(double value){
        if (mergeImage != null){
            double stepValue = value * 0.01;
            int r;
            int g;
            int b;
            Color imageTemp[][] = new Color[image.length][image[0].length];
            for (int row = 0; row < imageTemp.length; row ++){
                for (int col = 0; col < imageTemp[row].length; col ++){
                    //find averages of rgb values
                    r = (int)(stepValue*mergeImage[row][col].getRed() + (1-stepValue)*image[row][col].getRed());
                    g = (int)(stepValue*mergeImage[row][col].getGreen() + (1-stepValue)*image[row][col].getGreen());
                    b = (int)(stepValue*mergeImage[row][col].getBlue() + (1-stepValue)*image[row][col].getBlue());
                    //set colours to temp array
                    Color c = new Color(r, g, b);
                    imageTemp[row][col] = c;
                }
            }
            editedImage = copyImage(imageTemp);
            drawEditedImage();
        }
    }

    /**
     * crops the image and zooms in so that the user can have a closer look. The resultant image still maintains the same aspect ratio
     */
    public void cropZoom(){
        Color imageTemp[][] = new Color[editedImage.length][editedImage[0].length];
        double xCoord = selectRect[0];
        double yCoord = selectRect[1];
        double width = selectRect[2] - selectRect[0];
        double height = selectRect[3] - selectRect[1];
        double imageHeight = editedImage.length;
        double imageWidth = editedImage[0].length;

        if (height/width > imageHeight/imageWidth){ //if ratio of rect higher than image
            double oldWidth = width;
            width = height * imageWidth / imageHeight; //sets it to ratio of image
            xCoord = xCoord  - width/2 + oldWidth/2;
            if(xCoord < 0){ //go over the left side
                xCoord = 0.0;
            }
            else if(xCoord > imageWidth - width){ //go over the right side
                xCoord = imageWidth - width;
            }
        }
        if (height/width < imageHeight/imageWidth){ //if ratio of rect lower than image
            double oldHeight = height;
            height = width * imageHeight / imageWidth; //sets it to ratio of image
            yCoord = yCoord - height/2 + oldHeight/2;
            if(yCoord < 0){ //go over the top side
                yCoord = 0.0;
            }
            else if(yCoord > imageHeight - height){ //go over the bottom side
                yCoord = imageHeight - height;
            }
        }
        //creates the cropped array from the image that will be the source of the new image
        Color imageTempCrop[][] = new Color[(int)(height)][(int)(width)];
        for (int row = 0; row < imageTempCrop.length; row ++){
            for (int col = 0; col < imageTempCrop[row].length; col ++){
                imageTempCrop[row][col] = editedImage[(int)(row + yCoord)][(int)(col + xCoord)];
            }
        }
        //scales up the image
        double scaleMultiplier = imageHeight / height; //ratio between size of image and scale
        double r1, r2, r3, r4;
        double g1, g2, g3, g4;
        double b1, b2, b3, b4;
        double xPortion = scaleMultiplier;
        double yPortion = scaleMultiplier;
        double dx = 0; //overlapping colors in pixels on x axis
        double dy = 0; //overlapping colors in pixels on y axis
        int rowCrop = 0; //the coords of the image being scaled
        int colCrop = 0;
        for (int row = 0; row < imageTemp.length; row ++){
            for (int col = 0; col < imageTemp[row].length; col ++){
                if (rowCrop >= imageTempCrop.length - 1 &&  colCrop >= imageTempCrop[rowCrop].length - 1){//if bottom right corner
                    r1 = imageTempCrop[rowCrop][colCrop].getRed();
                    r2 = 0; 
                    r3 = 0; 
                    r4 = 0;

                    g1 = imageTempCrop[rowCrop][colCrop].getGreen();
                    g2 = 0; 
                    g3 = 0; 
                    g4 = 0;

                    b1 = imageTempCrop[rowCrop][colCrop].getBlue();
                    b2 = 0; 
                    b3 = 0; 
                    b4 = 0;

                    dy = 0;//is at bot right corner so only gathering from one pixel
                    dx = 0;
                }
                else if (rowCrop >= imageTempCrop.length - 1){ //if at bottom edge
                    r1 = imageTempCrop[rowCrop][colCrop].getRed();
                    r2 = imageTempCrop[rowCrop][colCrop+1].getRed();
                    r3 = 0;
                    r4 = 0;

                    g1 = imageTempCrop[rowCrop][colCrop].getGreen();
                    g2 = imageTempCrop[rowCrop][colCrop+1].getGreen();
                    g3 = 0;
                    g4 = 0;

                    b1 = imageTempCrop[rowCrop][colCrop].getBlue();
                    b2 = imageTempCrop[rowCrop][colCrop+1].getBlue();
                    b3 = 0;
                    b4 = 0;

                    dy = 0; //is a ty edge so there is only one pixel on the y axis being gathered
                }
                else if (colCrop >= imageTempCrop[rowCrop].length - 1){ //if at right edge
                    r1 = imageTempCrop[rowCrop][colCrop].getRed();
                    r2 = 0;
                    r3 = imageTempCrop[rowCrop+1][colCrop].getRed();
                    r4 = 0;

                    g1 = imageTempCrop[rowCrop][colCrop].getGreen();
                    g2 = 0;
                    g3 = imageTempCrop[rowCrop+1][colCrop].getGreen();
                    g4 = 0;

                    b1 = imageTempCrop[rowCrop][colCrop].getBlue();
                    b2 = 0;
                    b3 = imageTempCrop[rowCrop+1][colCrop].getBlue();
                    b4 = 0;

                    dx = 0; //is at x edge so there is only one pixel on the x axis being gathered
                }
                else{//any other pint of image
                    //get red values for the pixel
                    r1 = imageTempCrop[rowCrop][colCrop].getRed();
                    r2 = imageTempCrop[rowCrop][colCrop+1].getRed();
                    r3 = imageTempCrop[rowCrop+1][colCrop].getRed();
                    r4 = imageTempCrop[rowCrop+1][colCrop+1].getRed();
                    //get green values for the pixel
                    g1 = imageTempCrop[rowCrop][colCrop].getGreen();
                    g2 = imageTempCrop[rowCrop][colCrop+1].getGreen();
                    g3 = imageTempCrop[rowCrop+1][colCrop].getGreen();
                    g4 = imageTempCrop[rowCrop+1][colCrop+1].getGreen();
                    //get blue values for the pixel
                    b1 = imageTempCrop[rowCrop][colCrop].getBlue();
                    b2 = imageTempCrop[rowCrop][colCrop+1].getBlue();
                    b3 = imageTempCrop[rowCrop+1][colCrop].getBlue();
                    b4 = imageTempCrop[rowCrop+1][colCrop+1].getBlue();
                }
                //get average values for the color
                int red = (int)(r4*-dx*-dy + r3*(1+dx)*-dy + r2*-dx*(1+dy) + r1*(1+dx)*(1+dy));
                int green = (int)(g4*-dx*-dy + g3*(1+dx)*-dy + g2*-dx*(1+dy) + g1*(1+dx)*(1+dy));
                int blue = (int)(b4*-dx*-dy + b3*(1+dx)*-dy + b2*-dx*(1+dy) + b1*(1+dx)*(1+dy));

                Color c = new Color (red,green,blue);
                imageTemp[row][col] = c;

                xPortion -= 1;//moves one step
                if (xPortion < 0){
                    dx = xPortion;
                    xPortion = xPortion + scaleMultiplier;  //loads length that TempCrop pixel is used
                    if(colCrop < imageTempCrop[rowCrop].length - 1){//if at edge would keep at same pixel
                        colCrop++;
                    }
                }else {dx = 0;}
                if(col >= imageTemp[row].length - 1){//goes down to next row
                    colCrop = 0;
                    dx = 0;
                }
            }
            yPortion -= 1;//moves one step
            if (yPortion < 0){
                dy = yPortion; 
                yPortion += scaleMultiplier;  //loads length that TempCrop pixel is used
                if(rowCrop < imageTempCrop.length - 1){//if at edge would keep at same pixel
                    rowCrop++;
                    dy = 0;
                }
            }else {dy = 0;}
        }
        editedImage = copyImage(imageTemp);
        drawEditedImage();
    }

    /**
     * Adds the blur filter over the entire image. It is a convultion filter
     */
    public void blur(){
        Color imageTemp[][] = new Color[editedImage.length][editedImage[0].length];
        for (int row = 0; row < imageTemp.length; row ++){
            for (int col = 0; col < imageTemp[row].length; col ++){
                if(row > 0 && row < imageTemp.length-1 && col > 0 && col < imageTemp[row].length-1){
                    //find weighted red value of the blur
                    int red = (int)(editedImage[row-1][col-1].getRed() * 0.05 + editedImage[row-1][col].getRed() * 0.1 + editedImage[row-1][col+1].getRed() * 0.05 +
                            editedImage[row][col-1].getRed() * 0.1 + editedImage[row][col].getRed() * 0.4 + editedImage[row][col+1].getRed() * 0.1 +
                            editedImage[row+1][col-1].getRed() * 0.05 + editedImage[row+1][col].getRed() * 0.1 + editedImage[row+1][col+1].getRed() * 0.05);
                    //find weighted green value of the blur
                    int green = (int)(editedImage[row-1][col-1].getGreen() * 0.05 + editedImage[row-1][col].getGreen() * 0.1 + editedImage[row-1][col+1].getGreen() * 0.05 +
                            editedImage[row][col-1].getGreen() * 0.1 + editedImage[row][col].getGreen() * 0.4 + editedImage[row][col+1].getGreen() * 0.1 +
                            editedImage[row+1][col-1].getGreen() * 0.05 + editedImage[row+1][col].getGreen() * 0.1 + editedImage[row+1][col+1].getGreen() * 0.05);
                    //find weighted blue value of the blur
                    int blue = (int)(editedImage[row-1][col-1].getBlue() * 0.05 + editedImage[row-1][col].getBlue() * 0.1 + editedImage[row-1][col+1].getBlue() * 0.05 +
                            editedImage[row][col-1].getBlue() * 0.1 + editedImage[row][col].getBlue() * 0.4 + editedImage[row][col+1].getBlue() * 0.1 +
                            editedImage[row+1][col-1].getBlue() * 0.05 + editedImage[row+1][col].getBlue() * 0.1 + editedImage[row+1][col+1].getBlue() * 0.05);
                    //make resultant colour
                    Color c = new Color(red,green,blue);
                    imageTemp[row][col] = c;
                }else{
                    imageTemp[row][col] = editedImage[row][col];  
                }
            }
        }
        editedImage = copyImage(imageTemp);
        drawEditedImage();
    }

    /**
     * Rotates the image by a set amount of degrees, the angle according to value entered in the slider
     */
    public void rotate(double angle){
        angle = Math.toRadians(angle);

        Color imageTemp[][] = new Color[editedImage.length][editedImage[0].length];
        double r1, r2, r3, r4;
        double g1, g2, g3, g4;
        double b1, b2, b3, b4;
        double xStep = Math.abs(Math.cos(angle));
        double yStep = Math.abs(Math.sin(angle));
        double xCentre = editedImage[0].length / 2; //centre of the image
        double yCentre = editedImage.length / 2; //centre of the image

        double height = image.length;
        double width = image[0].length;
        for (int row = 0; row < imageTemp.length; row ++){
            for (int col = 0; col < imageTemp[row].length; col ++){
                //find the orignal x coordinates of the rotation

                double xOriginalDouble = xCentre + (col - xCentre)*Math.cos(angle) + (row - yCentre)*Math.sin(angle);
                double yOriginalDouble = yCentre - (col - xCentre)*Math.sin(angle) + (row - yCentre)*Math.cos(angle);

                int xOriginal = (int)xOriginalDouble;
                int yOriginal = (int)yOriginalDouble;

                double dx = Math.abs(xOriginalDouble - xOriginal);
                double dy = Math.abs(yOriginalDouble - yOriginal);
                //gather the orignal values of the original cell.
                if(xOriginal < -1 || yOriginal < -1 || xOriginal >= width || yOriginal >= height){ //pixel and adjacent ones are null
                    r1 = 0; r2 = 0; r3 = 0; r4 = 0; 
                    g1 = 0; g2 = 0; g3 = 0; g4 = 0;
                    b1 = 0; b2 = 0; b3 = 0; b4 = 0;
                }else if(xOriginal == -1 && yOriginal == -1){//top left corner
                    r1 = 0; r2 = 0; r3 = 0; r4 = image[yOriginal+1][xOriginal+1].getRed(); 
                    g1 = 0; g2 = 0; g3 = 0; g4 = image[yOriginal+1][xOriginal+1].getGreen(); 
                    b1 = 0; b2 = 0; b3 = 0; b4 = image[yOriginal+1][xOriginal+1].getBlue(); 
                }else if(xOriginal == width - 1 && yOriginal == -1){//top right corner
                    r1 = 0; r2 = 0; r3 = image[yOriginal+1][xOriginal].getRed(); r4 = 0; 
                    g1 = 0; g2 = 0; g3 = image[yOriginal+1][xOriginal].getGreen(); g4 = 0; 
                    b1 = 0; b2 = 0; b3 = image[yOriginal+1][xOriginal].getBlue(); b4 = 0; 
                }else if(xOriginal == -1 && yOriginal == height - 1){//bottom left corner
                    r1 = 0; r2 = image[yOriginal][xOriginal+1].getRed(); r3 = 0; r4 = 0; 
                    g1 = 0; g2 = image[yOriginal][xOriginal+1].getGreen(); g3 = 0; g4 = 0; 
                    b1 = 0; b2 = image[yOriginal][xOriginal+1].getBlue(); b3 = 0; b4 = 0; 
                }else if(xOriginal == width - 1 && yOriginal == height - 1){//botton right corner
                    r1 = image[yOriginal][xOriginal].getRed(); r2 = 0; r3 = 0; r4 = 0; 
                    g1 = image[yOriginal][xOriginal].getGreen(); g2 = 0; g3 = 0; g4 = 0; 
                    b1 = image[yOriginal][xOriginal].getBlue(); b2 = 0; b3 = 0; b4 = 0;
                }else if(yOriginal == - 1){//top side
                    r1 = 0; r2 = 0; r3 = image[yOriginal+1][xOriginal].getRed(); r4 = image[yOriginal+1][xOriginal+1].getRed(); 
                    g1 = 0; g2 = 0; g3 = image[yOriginal+1][xOriginal].getGreen(); g4 = image[yOriginal+1][xOriginal+1].getGreen(); 
                    b1 = 0; b2 = 0; b3 = image[yOriginal+1][xOriginal].getBlue(); b4 = image[yOriginal+1][xOriginal+1].getBlue();
                }else if(yOriginal == height - 1){//bottom side
                    r1 = image[yOriginal][xOriginal].getRed(); r2 = image[yOriginal][xOriginal+1].getRed(); r3 = 0; r4 = 0;
                    g1 = image[yOriginal][xOriginal].getGreen(); g2 = image[yOriginal][xOriginal+1].getGreen(); g3 = 0; g4 = 0;
                    b1 = image[yOriginal][xOriginal].getBlue(); b2 = image[yOriginal][xOriginal+1].getBlue(); b3 = 0; b4 = 0;
                }else if(xOriginal == -1){//left side
                    r1 = 0; r2 = image[yOriginal][xOriginal+1].getRed(); r3 = 0; r4 = image[yOriginal+1][xOriginal+1].getRed();
                    g1 = 0; g2 = image[yOriginal][xOriginal+1].getGreen(); g3 = 0; g4 = image[yOriginal+1][xOriginal+1].getGreen();
                    b1 = 0; b2 = image[yOriginal][xOriginal+1].getBlue(); b3 = 0; b4 = image[yOriginal+1][xOriginal+1].getBlue();
                }else if(xOriginal == width - 1){//right side
                    r1 = 0; r2 = image[yOriginal][xOriginal].getRed(); r3 = 0; r4 = image[yOriginal+1][xOriginal].getRed();
                    g1 = 0; g2 = image[yOriginal][xOriginal].getGreen(); g3 = 0; g4 = image[yOriginal+1][xOriginal].getGreen();
                    b1 = 0; b2 = image[yOriginal][xOriginal].getBlue(); b3 = 0; b4 = image[yOriginal+1][xOriginal].getBlue();
                }else{ //anywhere else
                    r1 = image[yOriginal][xOriginal].getRed(); r2 = image[yOriginal][xOriginal+1].getRed(); r3 = image[yOriginal+1][xOriginal].getRed(); r4 = image[yOriginal+1][xOriginal+1].getRed();
                    g1 = image[yOriginal][xOriginal].getGreen(); g2 = image[yOriginal][xOriginal+1].getGreen(); g3 = image[yOriginal+1][xOriginal].getGreen(); g4 = image[yOriginal+1][xOriginal+1].getGreen();
                    b1 = image[yOriginal][xOriginal].getBlue(); b2 = image[yOriginal][xOriginal+1].getBlue(); b3 = image[yOriginal+1][xOriginal].getBlue(); b4 = image[yOriginal+1][xOriginal+1].getBlue();
                }
                //find weighted average of the 4 pixels overlapping the pixel being drawn
                int red = (int)(Math.abs(r1*dx*dy + r2*(1-dx)*dy + r3*dx*(1-dy) + r4*(1-dx)*(1-dy)));
                int green = (int)(Math.abs(g1*dx*dy + g2*(1-dx)*dy + g3*dx*(1-dy) + g4*(1-dx)*(1-dy)));
                int blue = (int)(Math.abs(b1*dx*dy + b2*(1-dx)*dy + b3*dx*(1-dy) + b4*(1-dx)*(1-dy)));
                Color c = new Color (red,green,blue);
                imageTemp[row][col] = c;
            }
        }
        editedImage = copyImage(imageTemp);
        drawEditedImage();
    }

    /**
     * Allows the user to choose a colour
     */
    public void setPaint(){
        JColorChooser j = new JColorChooser();
        paint = j.showDialog(j, "Choose Colour", Color.white);
    }

    /**
     * enables the user to pour if they have selected a colour
     */
    public void pour(){
        if(paint!=null){
            toPour = true;
            toFilterBrush = false;
            toSelect = false;
        }else{
            UI.println("Choose a paint colour first");
        }
    }

    /**
     * does the pour action. turns series of pixels into the same colour as the paint
     */
    public void doPour(double x, double y){
        int localRowCol[] = rowCol(x,y);
        editedImage = copyImage(image);
        if (localRowCol != null){
            int row = localRowCol[0];
            int col = localRowCol[1];
            Color c = image[row][col];
            try{
                fill(row,col, c);
            }catch(StackOverflowError e){
                UI.println("The area pour was used on was too large");
            }

            drawEditedImage();
        }
    }

    /**
     * updates the pixel chosen to new colour given
     */
    private void updatePixel(int row, int col){
        editedImage[row][col] = paint;
    }

    /**
     * the actual method that fills in the pixels with the colour
     */
    private void fill(int row, int col, Color cl){
        if (row<0 || col<0 || col>=editedImage[0].length || row>=editedImage.length){return;}
        if (editedImage[row][col] == paint){return;}
        if (colorCheck(editedImage[row][col], cl)){ //colour tolerance
            updatePixel(row,col);
            fill(row+1,col, cl);
            fill(row-1,col, cl);
            fill(row,col+1, cl);
            fill(row,col-1, cl);
        }
        return;
    }

    /**
     * Changes the threshold of the pour function to a new value
     */
    public void setThreshold(double t){
        pourThreshold = (int)(t);
    }

    /**
     * Loads the convolution filter file onto the program
     */
    public void loadConvo(){
        String imageName = UIFileChooser.open();
        if (imageName != null){
            try {
                Scanner s = new Scanner(new File(imageName));
                UI.println(imageName + " filter has been loaded");
                int indexCount = s.nextInt();
                convoFilter = new Double[indexCount][indexCount];
                for (int i = 0; i < indexCount; i++){
                    for (int j = 0; j < indexCount; j++){
                        convoFilter[i][j] = s.nextDouble();
                    }
                }
            } catch(IOException e){UI.println("Image reading failed: "+e);}
        }  
    }

    /**
     * Uses the convolution filter on the image
     */
    public void useConvo(){
        if(convoFilter == null){
            UI.println("Load a new filter");
        }else{
            Color imageTemp[][] = new Color[editedImage.length][editedImage[0].length];      
            int halfLength = (int)(convoFilter.length / 2);
            for (int row = 0; row < imageTemp.length; row ++){
                for (int col = 0; col < imageTemp[row].length; col ++){
                    if(row > halfLength && row < imageTemp.length-halfLength && col > halfLength && col < imageTemp[row].length-halfLength){
                        //find weighted red value of the blur
                        double r = 0;
                        double g = 0;
                        double b = 0;
                        for (int convoRow = 0; convoRow < convoFilter.length; convoRow++){
                            for (int convoCol = 0; convoCol < convoFilter[0].length; convoCol++){
                                r += editedImage[row - halfLength + convoRow][col - halfLength + convoCol].getRed() * convoFilter[convoRow][convoCol];
                                g += editedImage[row - halfLength + convoRow][col - halfLength + convoCol].getGreen() * convoFilter[convoRow][convoCol];
                                b += editedImage[row - halfLength + convoRow][col - halfLength + convoCol].getBlue() * convoFilter[convoRow][convoCol];
                            }
                        }
                        //make resultant colour
                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));
                        Color c = new Color((int)r,(int)g,(int)b);
                        imageTemp[row][col] = c;
                    }else{
                        imageTemp[row][col] = editedImage[row][col];  
                    }
                }
            }
            editedImage = copyImage(imageTemp);
            drawEditedImage();   
        }
    }

    /**
     * this is the mouse method that translates the interactions of the mouse towards the buttons and functions used
     */
    public void doMouse(String event, double x, double y){
        double temp;
        if (event == "pressed"){
            if(toSelect){
                if (!drawnOver){
                    UI.invertRect(selectRect[0], selectRect[1], selectRect[2] - selectRect[0], selectRect[3] - selectRect[1]);
                    drawnOver = true;
                }
                selectRect[0] = x;
                selectRect[1] = y;
            }

        }        
        if (event == "released"){
            if(toSelect){
                selectRect[2] = x;
                selectRect[3] = y;
                if (selectRect[2] < selectRect[0]){
                    temp = selectRect[2];
                    selectRect[2] = selectRect[0];
                    selectRect[0] = temp;
                }
                if (selectRect[3] < selectRect[1]){
                    temp = selectRect[3];
                    selectRect[3] = selectRect[1];
                    selectRect[1] = temp;
                }
                UI.invertRect(selectRect[0], selectRect[1], selectRect[2] - selectRect[0], selectRect[3] - selectRect[1]); //draws the rectangle for the user
            }
        }
        if (event == "clicked"){ //
            if (toPour){//does the pour action
                doPour(x,y);
            }
        }
        if (event == "dragged"){
            if (rowCol(x,y) != null && toFilterBrush){
                filterBrush(rowCol(x,y)[0], rowCol(x,y)[1]);
            }
        }
    }

    /**
     * Activates the filter brush method, allowing the user to use the filter brush
     */
    public void enableFilterBrush(){
        if(convoFilter != null){
            toFilterBrush = true;
            toPour = false;
            toSelect = false;
        }
        else{
            UI.println("Must choose a convolution filter");
        }
    }

    /**
     * does the action of the filter brush
     */
    public void filterBrush(int row, int col){   //Bhoker Blur (gaussian blur) is the only one that works
        int halfLength = (int)(convoFilter.length / 2);
        for (int r = row - 3; r < row + 3; r ++){
            for (int c = col - 3; c < col + 3; c ++){
                if(row > halfLength + 3 && row < editedImage.length-halfLength -3 && col > halfLength + 3 && col < editedImage[row].length-halfLength - 3){
                    //find weighted red value of the blur
                    double red = 0;
                    double green = 0;
                    double blue = 0;
                    for (int convoRow = 0; convoRow < convoFilter.length; convoRow++){
                        for (int convoCol = 0; convoCol < convoFilter[0].length; convoCol++){
                            red += editedImage[r - halfLength + convoRow][c - halfLength + convoCol].getRed() * convoFilter[convoRow][convoCol];
                            green += editedImage[r - halfLength + convoRow][c - halfLength + convoCol].getGreen() * convoFilter[convoRow][convoCol];
                            blue += editedImage[r - halfLength + convoRow][c - halfLength + convoCol].getBlue() * convoFilter[convoRow][convoCol];
                        }
                    }
                    //make resultant colour
                    red = Math.max(0, Math.min(255, red));
                    green = Math.max(0, Math.min(255, green));
                    blue = Math.max(0, Math.min(255, blue));
                    Color cl = new Color((int)red,(int)green,(int)blue);

                    editedImage[r][c] = cl;
                    UI.setColor(editedImage[r][c]);
                    UI.fillRect(c, r, 1, 1);
                }
            }
        }
    }

    /**
     * Activates the select tool action
     */
    public void useSelectTool(){
        toSelect = true;
        toFilterBrush = false;
        toPour = false;
    }

    /**
     * is the equals method of a colour. It has a tolerance so some colours
     * that are slightly different will still be seen as equal.
     */
    public boolean colorCheck(Color a, Color b){
        int ar = a.getRed();
        int ag = a.getGreen();
        int ab = a.getBlue();
        int br = b.getRed();
        int bg = b.getGreen();
        int bb = b.getBlue();
        if (Math.abs(ar - br) > pourThreshold){return false;}
        if (Math.abs(ag - bg) > pourThreshold){return false;}
        if (Math.abs(ab - bb) > pourThreshold){return false;}
        return true;
    }

    /**
     * converts the x,y coordinates to row and col of the image
     */
    public int[] rowCol(double x, double y){
        int rowCol[] = new int[2]; //0th index is row, 1st index is col
        if (x<0 || x>editedImage[0].length || y<0 || y>editedImage.length){return null;}
        rowCol[0] = (int)(y);        
        rowCol[1] = (int)(x);
        return rowCol;
    }

    /**
     * commit the editedImage and the changes it has into the image object
     */
    public void commit(){
        UI.eraseRect(editedImage[0].length, 0, image[0].length, image.length);
        UI.eraseRect(0, 0, editedImage.length, editedImage[0].length);
        image = copyImage(editedImage);
        drawImage();
        drawEditedImage();
        mergeImage = null;
    }
}

