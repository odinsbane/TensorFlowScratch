package org.orangepalantir;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * First excercise from http://neuralnetworksanddeeplearning.com/chap1.html
 */
public class Network{

    //state of the network.
    int[] sizes;
    Random ng = new Random();
    List<double[]> bias = new ArrayList<>();
    List<double[][]> weights = new ArrayList<>();

    int success = 0;
    /**
     * Creates a new network with sizes.length layers. One is the input layer and the other is the output layer and
     * the rest are hidden.
     *
     * @param sizes an array with the number of nodes per layer.
     */
    public Network(int[] sizes){

        //initialize the bias to random values.
        for(int i = 1; i<sizes.length; i++){
            double[] b = new double[sizes[i]];
            for(int j = 0; j<b.length; j++){
                b[j] = ng.nextGaussian();
            }
            bias.add(b);
        }

        //initialize the weights to random values.
        for(int i = 1; i<sizes.length; i++){
            int x = sizes[i-1];
            int y = sizes[i];
            double[][] w = new double[y][x];
            for(int j = 0; j<x; j++){
                for(int k = 0; k<y; k++){
                    w[k][j] = ng.nextGaussian();
                }
            }
            weights.add(w);
        }

        this.sizes = sizes;

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

    /**
     *
     * @param trainingData List a list of (x, y) values. traingingData.get(i) returns an x, y value.
     * @param epochs
     * @param batchSize
     * @param eta
     * @param testData
     */
    public void stochasticGradientDescent(List<List<double[]>> trainingData, int epochs, int batchSize, double eta, List<List<double[]>> testData){
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
            if(testData!=null){
                int n = testData.size();
                success = evaluate(testData);
                System.out.println("Epoch: " + i + " " + success + "/" + n);
            }

        }
    }

    /**
     * Evaluates all of the test data using feed forward and comparing that to the expected values included in the test
     * data.
     *
     * @param testData A list of N samples, {input vector, output vector pairs}.
     * @return The total number of samples that the network got correct. a number betwen 0 and N.
     */
    public int evaluate(List<List<double[]>> testData){
        int sum = 0;
        for(List<double[]> data: testData){
            double[] x = data.get(0);
            double[] y = data.get(1);
            double[] a = feedForward(x);
            int num = maxIndex(y);
            int check = maxIndex(a);
            if(num==check){
                sum++;
            }
        }
        return sum;
    }

    public int maxIndex(double[] a){
        double max = a[0];
        int dex = 0;
        for(int i =1; i<a.length; i++){
            if(a[i]>max){
                max = a[i];
                dex = i;
            }
        }
        return dex;
    }

    static class BackPropagationResult{
        List<double[]> deltaNablaB;
        List<double[][]> deltaNablaW;
        public BackPropagationResult(List<double[]> deltaNablaB, List<double[][]> deltaNablaW){
            this.deltaNablaB = deltaNablaB;
            this.deltaNablaW = deltaNablaW;
        }
    }

    public void updateMiniBatch(List<List<double[]>> miniBatch, double trainingRate){
        List<double[]> nablaB = getBiasZeros();

        List<double[][]> nablaW = getWeightZeros();

        for(List<double[]> batch: miniBatch){
            double[] x = batch.get(0);
            double[] y = batch.get(1);
            BackPropagationResult result = backPropagate(x, y);
            for(int i = 0; i<bias.size(); i++){
                double[] nb = nablaB.get(i);
                double[] dnb = result.deltaNablaB.get(i);
                for(int j = 0; j<nb.length; j++){
                    nb[j] += dnb[j];
                }
            }
            for(int i = 0; i<weights.size(); i++){
                double[][] nw = nablaW.get(i);
                double[][] dnw = result.deltaNablaW.get(i);
                for(int j = 0; j<nw.length; j++){
                    double[] nwr = nw[j];
                    double[] dnwr = dnw[j];
                    for(int k = 0; k<nwr.length; k++){
                        nwr[k] += dnwr[k];
                    }
                }
            }
        }
        double factor = trainingRate/miniBatch.size();
        for(int i = 0; i<weights.size(); i++){
            double[][] weight = weights.get(i);
            double[][] nW = nablaW.get(i);

            double[] b = bias.get(i);
            double[] nb = nablaB.get(i);


            for(int j = 0; j< nW.length; j++){
                double[] wr = weight[j];
                double[] nwr = nW[j];
                for(int k = 0; k<wr.length; k++){
                    wr[k] = wr[k] - factor*nwr[k];
                }

                b[j] = b[j] - factor*nb[j];
            }
        }

    }

    /**
     * Calculate derivatives.
     *
     * @param x input vector.
     * @param y expected output.
     * @return nabla B, nabla W. The gradient for the cost function C_x.
     */
    public BackPropagationResult backPropagate(double[] x, double[] y){
        List<double[]> nablaB = getBiasZeros();
        List<double[][]> nablaW = getWeightZeros();

        List<double[]> zs= new ArrayList<>();
        List<double[]> activations = new ArrayList<>();

        activations.add(x);

        int layers = sizes.length;

        //evaluates the current network for the input x.
        double[] activation = x;
        for(int i = 0; i<layers-1; i++){

            double[] b = bias.get(i);
            double[][] w = weights.get(i);
            double[] z = argument(w, activation, b);
            zs.add(z);

            activation = sigmoid(z);
            activations.add(activation);

        }


        //calculates the difference between the last activation, and the actual values.
        double[] delta = costDerivative(activation, y);

        double[] derivatives = sigmoidDerivative(zs.get(zs.size()-1));

        double[] nB = nablaB.get(nablaB.size()-1);

        for(int i = 0; i<delta.length; i++){
            nB[i] = derivatives[i]*delta[i];
        }
        delta = nB;

        double[] act = activations.get(activations.size()-2);
        double[][] nW = nablaW.get(nablaW.size()-1);

        //deltas (dot) activations(Transpose)
        for(int i = 0; i<nW.length; i++){
            double[] nw = nW[i];
            for(int j = 0; j<nw.length; j++){
                nw[j] = nB[i]*act[j];
            }
        }


        for(int i = 2; i<layers; i++){
            double[] z = zs.get(zs.size()-i);
            double[] sp = sigmoidDerivative(z);

            double[][] weight = weights.get(weights.size()-i + 1);
            double[] del = nablaB.get(nablaB.size() - i);

            // weight(Transpose) (dot) delta
            for(int j = 0; j<weight.length; j++){
                double[] row = weight[j];
                for(int k = 0; k<row.length; k++){
                    del[k] += row[k]*delta[j];
                }
            }
            for(int j = 0; j<del.length; j++){
                del[j] = del[j]*sp[j];
            }
            delta = del;

            double[][] nw = nablaW.get(nablaW.size() - i);
            act = activations.get(activations.size() - i - 1);
            //delta (dot) activations(Transpose)
            for(int j = 0; j<nw.length; j++){
                double[] row = nw[j];
                for(int k = 0; k<row.length; k++){
                    row[k] =  delta[j]*act[k];
                }
            }
        }


        return new BackPropagationResult(nablaB, nablaW);


    }



    public double[] costDerivative(double[] activations, double[] y){
        double[] ret = new double[y.length];
        for(int i = 0; i<y.length; i++){
            ret[i] = activations[i] - y[i];
        }
        return ret;
    }

    /**
     * Gets a collection of zeroed arrays with the shape of bias.
     * @return List containing zeros.
     */
    List<double[]> getBiasZeros(){
        List<double[]> x = new ArrayList<>();
        for(int i = 0; i<bias.size(); i++){
            int n = bias.get(i).length;
            x.add(new double[n]);
        }
        return x;
    }

    List<double[][]> getWeightZeros(){
        List<double[][]> zeros = new ArrayList<>();

        for(int i = 0; i<weights.size(); i++){
            int wl = weights.get(i).length;
            int ww = weights.get(i)[0].length;
            zeros.add(new double[wl][ww]);
        }

        return zeros;
    }

    /**
     * Calculates the weighted values.
     * @param w
     * @param a
     * @param b
     * @return
     */
    public static double[] argument(double[][] w, double[] a, double[] b){
        double[] aprime = new double[b.length];
        for(int i = 0; i<b.length; i++){

            double[] weights = w[i];
            for(int j = 0; j<a.length; j++){
                aprime[i] += weights[j]*a[j];
            }
            aprime[i] += b[i];
        }
        return aprime;
    }

    /**
     * Calculates the output of sigmoid neurons for
     * @param w
     * @param a
     * @param b
     * @return
     */
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

    /**
     * Calculates the output of sigmoid neurons for
     * @return
     */
    public static double[] sigmoid(double[] z){
        double[] aprime = new double[z.length];
        for(int i = 0; i<z.length; i++){
            aprime[i] = 1.0/(1.0 + Math.exp(-z[i]));
        }
        return aprime;
    }

    public static double[] sigmoidDerivative(double[] zs){
        double[] aprime = new double[zs.length];
        for(int i = 0; i<zs.length; i++){
            double z = Math.exp(-zs[i]);
            if(z>Double.MAX_VALUE){
                aprime[i] = 0;
            } else {
                aprime[i] = z / ((1.0 + z) * (1.0 + z));
            }
        }
        return aprime;
    }


    public static double[] sigmoidDerivative(double[][] w, double[] a, double[] b){
        double[] aprime = new double[b.length];
        for(int i = 0; i<b.length; i++){

            double[] weights = w[i];
            for(int j = 0; j<a.length; j++){
                aprime[i] += weights[j]*a[j];
            }
            aprime[i] += b[i];
            double z = Math.exp(-aprime[i]);
            aprime[i] = z/((1.0 + z)*(1.0+z));
        }
        return aprime;
    }

    public static void main(String[] args) {
        Network n = new Network(new int[]{4, 3, 2});
        List<List<double[]>> training = new ArrayList<>();
        for(int i = 0; i<10; i++){

            double[] xn = new double[4];
            double[] yn = new double[2];
            for(int j = 0; j<4; j++){
                xn[j] = (i+j)%4;
            }

            for(int k = 0; k<2; k++){
                yn[k] = (i+k)%2;
            }

            training.add(Arrays.asList(xn, yn));

        }

        n.stochasticGradientDescent(training, 100, 5, 1, null);
        for (double[] bia : n.bias) {
            System.out.println(Arrays.toString(bia));
        }
        System.out.println("**************                     ****************");
        System.out.println("**************                     ****************");
        for(double[][] w: n.weights){
            for(double[] row: w){
                System.out.println(Arrays.toString(row));
            }
            System.out.println("**********");
        }
    }
}