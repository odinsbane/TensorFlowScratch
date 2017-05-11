package org.orangepalantir;

import sun.awt.image.BytePackedRaster;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
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
    Random ng = new Random(1l);

    class Network{
        int[] sizes;
        List<double[]> bias = new ArrayList<>();
        List<double[][]> weights = new ArrayList<>();
        public Network(int[] sizes){
            for(int i = 1; i<sizes.length; i++){
                double[] b = new double[sizes[i]];
                for(int j = 0; j<b.length; j++){
                    b[j] = ng.nextGaussian();
                }
                bias.add(b);
            }
            for(int i = 1; i<sizes.length; i++){
                int x = sizes[i-1];
                int y = sizes[i];
                double[][] w = new double[y][x];
                for(int j = 0; j<x; j++){
                    for(int k = 0; k<y; k++){
                        w[j][k] = ng.nextGaussian();
                    }
                }
            }

        }

        /**
         * Takes inputs a (N) calculates the final response and returns the output.
         *
         * @param a
         * @return
         */
        public double[] feedForward(double[] a){
            for(int i= 0; i<bias.size();i++){
                a = sigmoid(weights.get(i), a, bias.get(i));
            }
            return a;
        }

        public void StochasticGradientDescent(List<List<double[]>> trainingData, int epochs, int batchSize, double eta){
            for(int i = 0;i<epochs; i++){
                Collections.shuffle(trainingData);

                List<List<List<double[]>>> miniBatches = new ArrayList<>();
                int batchCount = trainingData.size()/batchSize;

                for(int k = 0; k<batchCount; k++){
                    miniBatches.add(trainingData.subList(k*batchSize, (k+1)*batchSize));
                }

                for(List<List<double[]>> miniBatch: miniBatches){
                    updateMiniBatch(miniBatch, eta);
                }


            }
        }
        public void updateMiniBatch(List<List<double[]>> miniBatch, double trainingRate){
            
        }
    }

    public static double[] sigmoid(double[][] w, double[] a, double[] b){
        double[] aprime = new double[b.length];
        for(int i = 0; i<b.length; i++){

            double[] weights = w[i];
            for(int j = 0; j<a.length; j++){
                aprime[i] += weights[j]*a[j];
            }
            aprime[i] += b[i];
            aprime[i] = 1.0/(1.0 + Math.exp(-aprime[i]));
        }
        return aprime;
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
                InputStream testImages = new GZIPInputStream(new FileInputStream(new File("training/train-images-idx3-ubyte.gz")));
                InputStream testLabels = new GZIPInputStream(new FileInputStream(new File("training/train-images-idx3-ubyte.gz")))

                ){
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

            List<BufferedImage> images = new ArrayList<>();
            List<String> labels = new ArrayList<>();


            int rows = trainImages.readInt();
            int columns = trainImages.readInt();;
            int n = rows*columns;

            byte[] lDat = new byte[items];
            trainLabels.readFully(lDat, 0, items);



            byte[] buffer = new byte[n];
            for(int i = 0; i<items; i++){

                trainImages.readFully(buffer);

                labels.add(String.format("%d", lDat[i]));

                BufferedImage img = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_GRAY);
                Raster r = Raster.createRaster(
                        img.getSampleModel(),
                        new DataBufferByte(buffer, rows*columns),
                        new Point(0,0)
                );
                img.setData(r);
                images.add(img);
            }

            showImages(images, labels);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MagicNumberException e){

        }


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
            int c = counter.accumulateAndGet(1, (i,x)->{
                return (i+x)%images.size();
            });

            button.setIcon(new ImageIcon(images.get(c)));
            label.setText(labels.get(c));
            frame.repaint();
        });
    }




}

class MagicNumberException extends Exception{
    MagicNumberException(String message){
        super(message);
    }
}
