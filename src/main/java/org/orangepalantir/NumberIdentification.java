package org.orangepalantir;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * For loading the MNIST handwritten digits data, found at http://yann.lecun.com/exdb/mnist/
 *
 * Created by msmith on 11.05.17.
 */
public class NumberIdentification {

    public NumberIdentification(){

    }


    final static int TRAIN_IMAGES_MAGIC=2051;
    final static int TRAIN_LABEL_MAGIC=2049;
    final static int TEST_IMAGES_MAGIC=2051;
    final static int TEST_LABEL_MAGIC=2049;


    public static void main(String[] args){
        try(
                DataInputStream trainImages = new DataInputStream(
                        new GZIPInputStream(new FileInputStream(new File("training/train-images-idx3-ubyte.gz")))
                );
                DataInputStream trainLabels = new DataInputStream(
                        new GZIPInputStream(new FileInputStream(new File("training/train-labels-idx1-ubyte.gz")))
                );
                DataInputStream testImages = new DataInputStream(
                        new GZIPInputStream(new FileInputStream(new File("training/t10k-images-idx3-ubyte.gz")))
                );
                DataInputStream testLabels = new DataInputStream(
                        new GZIPInputStream(new FileInputStream(new File("training/t10k-labels-idx1-ubyte.gz")))
                )

                ){

            //load test data.

            List<List<double[]>> trainingData = loadDataSet(trainImages, trainLabels);
            List<List<double[]>> testData = loadDataSet(testImages, testLabels);

            Network network = new Network(new int[]{784, 30, 10});
            network.stochasticGradientDescent(trainingData, 30, 10, 0.01, testData);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MagicNumberException e){
            e.printStackTrace();
        }


    }

    static List<List<double[]>> loadDataSet(DataInputStream trainImages, DataInputStream trainLabels) throws IOException, MagicNumberException {
        //readBytes(trainImages, buffer, 0, 8);
        //int magic = getInt(buffer, 0);
        int magic = trainImages.readInt();
        if(magic!=TRAIN_IMAGES_MAGIC){
            throw new MagicNumberException("Magic number not correct for training image file.");
        }
        int items = trainImages.readInt();

        magic = trainLabels.readInt();
        if(magic!=TRAIN_LABEL_MAGIC){
            throw new MagicNumberException("Magic number not correct for training label file.");
        }
        if(items!=trainLabels.readInt()){
            throw new MagicNumberException("Number of images does not equal the number of labels");
        }



        int rows = trainImages.readInt();
        int columns = trainImages.readInt();;
        int n = rows*columns;

        byte[] lDat = new byte[items];
        trainLabels.readFully(lDat, 0, items);


        byte[] buffer = new byte[n];
        List<List<double[]>> trainingData = new ArrayList<>();
        for(int i = 0; i<items; i++){

            trainImages.readFully(buffer);
            List<double[]> row = new ArrayList<>();
            row.add(convertToInput(buffer));
            double[] v = new double[10];
            v[lDat[i]] = 1;
            row.add(v);
            trainingData.add(row);
        }
        return trainingData;
    }

    static BufferedImage createImage(byte[] buffer, int columns, int rows){
        BufferedImage img = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_GRAY);
        Raster r = Raster.createRaster(
                img.getSampleModel(),
                new DataBufferByte(buffer, rows*columns),
                new Point(0,0)
        );
        img.setData(r);
        return img;
    }

    static void showImages(List<BufferedImage> images, List<String> labels){
        JFrame frame = new JFrame();
        ImageIcon icon = new ImageIcon(images.get(0));
        JButton button = new JButton();
        button.setIcon(icon);
        JLabel label = new JLabel(labels.get(0));
        frame.add(button, BorderLayout.CENTER);
        frame.add(label, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        AtomicInteger counter = new AtomicInteger(0);
        button.addActionListener(evt->{
            int c = counter.accumulateAndGet(1, (i,x)-> (i+x)%images.size());

            button.setIcon(new ImageIcon(images.get(c)));
            label.setText(labels.get(c));
            //frame.repaint();
        });
    }

    static double[] convertToInput(byte[] bytes){
        double[] ret = new double[bytes.length];
        for(int i = 0; i<bytes.length; i++){
            ret[i] = 1.0*(bytes[i]&0xff);
        }
        return ret;

    }



}

class MagicNumberException extends Exception{
    MagicNumberException(String message){
        super(message);
    }
}
