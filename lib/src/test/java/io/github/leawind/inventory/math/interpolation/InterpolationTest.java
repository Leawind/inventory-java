package io.github.leawind.inventory.math.interpolation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class InterpolationTest {

  public static JFrame showFigure(
      double xmin, double xmax, int count, Function<Double, Double> func) {

    double[] xs = new double[count];
    double[] ys = new double[count];

    double ymin = Double.POSITIVE_INFINITY;
    double ymax = Double.NEGATIVE_INFINITY;

    double step = (xmax - xmin) / count;
    for (int i = 0; i < count; i++) {
      xs[i] = xmin + i * step;
      ys[i] = func.apply(xs[i]);
      ymin = Math.min(ymin, ys[i]);
      ymax = Math.max(ymax, ys[i]);
    }

    final double YMIN = ymin;
    final double YMAX = ymax;

    JFrame frame = new JFrame("Figure");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.add(
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.BLACK);

            // x axis
            int y0 = (int) (getHeight() * (1 - (0 - YMIN) / (YMAX - YMIN)));
            g2d.drawLine(0, y0, getWidth(), y0);

            // y axis
            int x0 = (int) (getWidth() * (0 - xmin) / (xmax - xmin));
            g2d.drawLine(x0, 0, x0, getHeight());

            g2d.setColor(Color.BLUE);
            for (int i = 0; i < count; i++) {
              double xr = (xs[i] - xmin) / (xmax - xmin);
              double yr = 1 - (ys[i] - YMIN) / (YMAX - YMIN);

              int coordX = (int) (getWidth() * xr);
              int coordY = (int) (getHeight() * yr);

              g2d.fillRect(coordX, coordY, 5, 5);
            }
          }
        });

    frame.setVisible(true);

    return frame;
  }

  public static void showFigureAndAwait(
      double xmin, double xmax, int count, Function<Double, Double> func) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    JFrame frame = showFigure(xmin, xmax, count, func);

    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            future.complete(null);
          }
        });

    new Timer()
        .schedule(
            new TimerTask() {
              @Override
              public void run() {
                future.complete(null);
              }
            },
            10000);

    future.join();
  }

  public static void main(String[] args) {
    showFigure(-1 * Math.PI, 2 * Math.PI, 1024, Math::sin);
  }
}
