package org.orangepalantir.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * For representing a network with line drawings.
 *
 * Created by msmith on 17/05/17.
 */
public class LineDrawing {

    int height = 1024;
    int width = 1024;
    int padX = 50;
    int padY = 10;

    List<double[][]> weights;
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    public LineDrawing(){

    }

    public void setData(List<double[][]> weights){
        this.weights = weights;
        for(double[][] ws: weights){

            for(double[] r: ws){
                for(double w: r){
                    max = w>max?w:max;
                    min = w<min?w:min;
                }
            }

        }

    }



    public BufferedImage generateChart(){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0,0,width, height);
        int layers = weights.size();
        int dx = (width - padX)/layers;
        int offsetX = padX/2;
        int offsetY = padY/2;
        for(int i = 0; i<layers; i++){

            double[][] w = weights.get(i);
            int target = w.length;
            int origin = w[0].length;

            double oDy = (height - padY)*1.0/origin;
            double tDy = (height - padY)*1.0/target;
            System.out.println(oDy + ", " + tDy);
            int x1 = offsetX + i*dx;
            int x2 = x1 + dx;

            for(int j = 0; j<target; j++){
                int y2 = (int)(j*tDy + offsetY);
                for(int k = 0; k<origin; k++){
                    double v = w[j][k];
                    g2d.setColor(getColor(v));
                    int y1 = (int)(k*oDy + offsetY);
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

        }

        return img;
    }

    public Color getColor(double v){

        if(v<0){
            float a = (float)(0.5*v/min + 0.5);

            return new Color(1f, 0f, 0f, a);
        } else{
            float a = (float)(0.5*v/max + 0.5);
            return new Color(0f, 0f, 1f, a);
        }
    }

}
