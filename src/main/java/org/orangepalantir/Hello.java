package org.orangepalantir;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

public class Hello {

    public static void getStarted() throws IOException {
        Graph g = new Graph();
        Session s = new Session(g);

        Tensor node1 = Tensor.create(1.0f);
        Tensor node2 = Tensor.create(4.0f);


        System.out.println(node1.floatValue());
        System.out.println(node2.floatValue());
        Operation o1 = g.opBuilder("Const", "node1").setAttr("dtype", node1.dataType()).setAttr("value", node1).build();
        Operation o2 = g.opBuilder("Const", "node2").setAttr("dtype", node2.dataType()).setAttr("value", node2).build();
        Operation node3 = g.opBuilder("Add", "sum").addInput(o1.output(0)).addInput(o2.output(0)).build();
        System.out.println(node3);
        List<Tensor<?>> results = s.runner().fetch("sum").run();
        System.out.println(results.get(0).floatValue());


    }

    public static void helloWorld() throws UnsupportedEncodingException {
        try (Graph g = new Graph()) {
            final String value = "Hello from " + TensorFlow.version();

        // Construct the computation graph with a single operation, a constant
        // named "MyConst" with a value "value".
            try (Tensor t = Tensor.create(value.getBytes("UTF-8"))) {
                // The Java API doesn't yet include convenience functions for adding operations.
                g.opBuilder("Const", "MyConst").setAttr("dtype", t.dataType()).setAttr("value", t).build();
            }

            // Execute the "MyConst" operation in a Session.
            try (
                Session s = new Session(g);
                Tensor output = s.runner().fetch("MyConst").run().get(0)
            ) {
                System.out.println(new String(output.bytesValue(), "UTF-8"));
            }
        }

    }

    public static void main(String[] args) throws Exception {
        getStarted();
    }
}

