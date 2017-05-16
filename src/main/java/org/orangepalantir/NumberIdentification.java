package org.orangepalantir;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
    List<List<double[]>> trainingData;
    List<List<double[]>> testData;
    Network network;
    int epochs = 1;
    int hiddenLayer = 25;
    int batchSize = 10;
    double eta = 3;

    BufferedImage testImg;
    String testLabel;
    int testIndex;
    Random ng = new Random();
    public NumberIdentification(List<List<double[]>> trainingData, List<List<double[]>> testData){
        this.trainingData = trainingData;
        this.testData = testData;

    }

    public void start(){
        chooseRandomTestImage();
        EventQueue.invokeLater(()->{
            buildGui();
        });
    }

    public void chooseRandomTestImage(){

        testIndex = ng.nextInt(testData.size());
        List<double[]> test = testData.get(testIndex);
        testImg = createImage(test.get(0), 28, 28);
        testLabel = getLabel(test.get(1));
    }

    private String getLabel(double[] binary) {
        for(int i = 0; i<binary.length; i++){
            if(binary[i]>0){
                return i +"";
            }
        }
        return -1 + "";
    }

    public void initialize(){
        network = new Network(new int[]{784, hiddenLayer, 10});
    }
    public void trainNetwork(){
        network.stochasticGradientDescent(trainingData, epochs, batchSize, eta, testData);
    }

    final static int TRAIN_IMAGES_MAGIC=2051;
    final static int TRAIN_LABEL_MAGIC=2049;


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

            NumberIdentification numb = new NumberIdentification(trainingData, testData);
            numb.start();

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
        int columns = trainImages.readInt();
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

    static BufferedImage createImage(double[] value, int columns, int rows){
        byte[] buffer = new byte[value.length];
        for(int i = 0; i<value.length; i++){
            buffer[i] = (byte)(255*value[i]);
        }
        BufferedImage img = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_GRAY);
        Raster r = Raster.createRaster(
                img.getSampleModel(),
                new DataBufferByte(buffer, rows*columns),
                new Point(0,0)
        );
        img.setData(r);
        return img;
    }

    void buildGui(){
        JFrame frame = new JFrame();
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        ImageIcon icon = new ImageIcon();
        icon.setImage(testImg);
        JLabel testImage = new JLabel(icon);

        content.add(testImage, BorderLayout.CENTER);

        JLabel testJLabel = new JLabel(testLabel);
        JLabel guessJLabel = new JLabel("-");
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
        column.add(new JLabel("actual"));
        column.add(testJLabel);
        column.add(new JLabel("guess"));
        column.add(guessJLabel);
        content.add(column, BorderLayout.EAST);

        JPanel row = new JPanel();
        row.setLayout(new GridLayout(2,4));
        JButton initialize = new JButton("init");
        JButton train = new JButton("train");
        JButton guess = new JButton("guess");
        JButton next = new JButton("next");



        row.add(initialize);
        row.add(train);
        row.add(guess);
        row.add(next);

        JLabel successLabel = new JLabel("--");
        JLabel totalLabel = new JLabel("/" + testData.size());
        row.add(new JLabel(""));
        row.add(new JLabel(""));
        row.add(successLabel);
        row.add(totalLabel);

        content.add(row, BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initialize.addActionListener(evt->initialize());
        train.addActionListener(evt->{
            trainNetwork();
            successLabel.setText(network.success + "");
        });

        guess.addActionListener(evt->{
            List<double[]> data = testData.get(testIndex);
            double[] a = network.feedForward(data.get(0));
            double max = -Double.MAX_VALUE;
            int dex = -1;
            for(int i = 0; i<a.length; i++){
                if(a[i]>max){
                    dex = i;
                    max = a[i];
                }
            }
            guessJLabel.setText("" + dex);
        });

        next.addActionListener(evt->{
            chooseRandomTestImage();
            icon.setImage(testImg);
            testJLabel.setText("" + testLabel);
            testImage.repaint();
        });
    }

    static double[] convertToInput(byte[] bytes){
        double[] ret = new double[bytes.length];
        double v = 0;
        for(int i = 0; i<bytes.length; i++){
            ret[i] = 1.0*(bytes[i]&0xff)/255;

        }
        return ret;
    }




}

class MagicNumberException extends Exception{
    MagicNumberException(String message){
        super(message);
    }
}
