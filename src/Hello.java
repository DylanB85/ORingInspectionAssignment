import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.*;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class Hello {
	public static void main(String[]args)
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		JFrame frame = new JFrame("OpenCV");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel imageHolder = new JLabel();
		frame.getContentPane().add(imageHolder, BorderLayout.CENTER);
		
		
		frame.pack();
		frame.setVisible(true);
		
		frame.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode()== KeyEvent.VK_Q)
					System.exit(0);
			}
			public void keyReleased(KeyEvent arg0) {
				
			}
			public void keyTyped(KeyEvent arg0) {
				
			}
		});
		
		String streamAddr = "http://c-cam.uchicago.edu/mjpg/video.mjpg";
		
		VideoCapture vcap = new VideoCapture();
		if(!vcap.open(streamAddr))
		{
			System.out.println("Error open video stream");
			return;
		}
		
		System.out.println("Stream opened");
		Mat img = new Mat();
		Mat out = new Mat();
		
		while(true)
		{
			if(!vcap.read(img)) {
				System.out.println("No frame");
			} else {
				double before = (double)System.nanoTime()/1000000000;
				
				Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
				int t=100;
				threshold(img, t);
				Imgproc.cvtColor(img, out, Imgproc.COLOR_GRAY2BGR);
				double after =(double)System.nanoTime()/1000000000;
				Imgproc.putText(out, "Processing time: " + String.format("%.4f", after-before) + " secs ", new Point(20, 20), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0));
				BufferedImage jimg = Mat2BufferedImage(out);
				imageHolder.setIcon(new ImageIcon (jimg));
				frame.pack();
			}
		}
	}
	
	public static void threshold(Mat img, int t)
	{
		byte data[] = new byte[img.rows()*img.cols()*img.channels()];
		img.get(0, 0, data);
		for(int i=0; i<data.length;i++)
		{
			int unsigned = (data[i] & 0xff);
			if (unsigned > t)
				data[i] = (byte)255;
			else
				data[i] = (byte)0;
		}
		img.put(0, 0, data);
	}

	public static BufferedImage Mat2BufferedImage(Mat m)
	{
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if(m.channels()>1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0,b);
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}
}
