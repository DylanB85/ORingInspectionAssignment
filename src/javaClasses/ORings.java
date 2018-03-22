package javaClasses;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class ORings {
	
	public static void main(String[]args)
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		JFrame frame =  new JFrame("OpenCv");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel imageHolder = new JLabel();
		JLabel histogramHolder = new JLabel();
		
		frame.getContentPane().add(histogramHolder, BorderLayout.NORTH);
		frame.getContentPane().add(imageHolder, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		
		frame.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode()== KeyEvent.VK_Q)
					System.exit(0);
			}
			public void keyReleased(KeyEvent arg0) {
				
			}
			public void keyTyped(KeyEvent arg0) {
				
			}
		});
		
		System.out.println("Stream Opened");
		
		Mat img = new Mat();
		Mat out = new Mat();
		Mat histim = new Mat(256,256, CvType.CV_8UC3);
		
		int edges[] = null;
		int curlab[] = null;
		int oRingLabel;
		boolean pass;
		
		int i=1;
		while (true)
		{
			img = Imgcodecs.imread("C:\\Users\\dylan\\CompVision\\CompVisionAssignment1\\src\\images\\ORing" + (i%16) + ".jpg");
			int [] h = hist(img);
			
			System.out.println("Oring: " + i);
			
			double before = (double)System.nanoTime()/1000000000;
			
			Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
			
			drawHist(histim, h);
			
			
			threshold(img, findThreshold(h)-50);//threshold is the peak of histogram
			
			
			//fill gaps which appear in the ring
			fill(img);
			
			//remove any outliers around the ring
			erode(img);
			
			curlab = currentLabel(img);
			oRingLabel = countORing(img, curlab);
			
			//find the rings edges
			edges = ringEdges(img, curlab, oRingLabel);
			
			// remove anything outside of the ring
			noiseRemover(img, oRingLabel, curlab);
			
			pass = processDefect (img, edges, curlab);
			
			Imgproc.cvtColor(img, out, Imgproc.COLOR_GRAY2BGR);
			double after = (double)System.nanoTime()/1000000000;
			
			Imgproc.putText(out, "Processing ", new Point(20,20) , Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0));
			Imgproc.putText(out, String.format("%.4f", after-before) + " secs", new Point(0, 40) , Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0));
			
			
			if(pass)
			Imgproc.putText(out, "Passed", new Point(10,60), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,0));
			else {
				Imgproc.putText(out, "Failed", new Point(10,60), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,0,255));
			}
			
			BufferedImage jimg = Mat2BufferedImage(out);
			BufferedImage histo = Mat2BufferedImage(histim);
			imageHolder.setIcon(new ImageIcon(jimg));
			histogramHolder.setIcon(new ImageIcon(histo));
			frame.pack();
			i++;
			
			//Loops images back around to prevent program from crashing
			if (i == 16)
				i = 1;
	
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
		
	}


	public static int sumsArray(int[]array) {
		int sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		
		return sum;
		
	}
	
	public static boolean imageResult(Mat img, int centre, int max, int min, int[] curlab) {
		int[] rightPoint;
		int[] leftPoint;
		
		int sumRight = 0;
		int sumLeft = 0;
		double defect = 6;
		
		int current = min;
		
		boolean passImage = true;
		
		while(current <= max) {
			leftPoint = new int[centre];
			rightPoint = new int[img.cols() - centre];
			
			for(int i = 0; i < leftPoint.length; i++) {
				int point = (current * img.rows()) + i; 
				
				if(curlab[point] == 1) {
					leftPoint[i] = 1;
				} else {
					leftPoint[i] = 0;
				}
			}
			
			for(int j = rightPoint.length - 1; j >= 0; j--) {
				int point = ((current * img.rows()) + (img.cols() - j));
				
				if(curlab[point] == 1) {
					rightPoint[j] = 1;
				} else {
					rightPoint[j] = 0;
				}
			}
			
			
			sumRight = sumsArray(rightPoint);
			sumLeft = sumsArray(leftPoint);
			
			current++;
		
			if(Math.abs(sumRight - sumLeft) <= defect) {
				sumRight = 0;
				sumLeft = 0;
			} else {
				passImage = false;
			}
		}
		
		return passImage;	
	}
	
	public static boolean processDefect(Mat img, int[] edges, int[] curlab)
	{
		
		int highPoint = 0;
		int lowPoint = 0;
		int leftPoint = 0;
		int rightPoint = 0;
		
		int maxMajor = 0;
		int minMajor = img.cols();
		int maxMinor = 0;
		int minMinor = img.rows();
		
		for(int i = 0; i < edges.length; i++) {
			int x = 0;
			int y = 0;
			
			if(i > 0) {
				y = (int)(Math.floor((i / img.rows())));
				x = ((y * img.rows()) - i) * (-1);
			}
			
				if(edges [i] == 1) {
					if(edges [i - img.cols()] == 0 && y < minMajor) {
					highPoint = i;
					minMajor = y;						
				}
				
				if(edges[i + 1] == 0 && edges[i + img.cols()] == 1 && x > maxMinor) {
					rightPoint = i;
					maxMinor= x;
				}
				
				if(edges[i + img.cols()] == 0 && y > maxMajor) {
					lowPoint = i;
					maxMajor = y;
				}
				
				if(edges[i - 1] == 0 && edges[i - img.cols()] == 1 && x < minMinor) {
					leftPoint = i;
					minMinor = x;
				}
			}
		}
		
		int minCentre = ((maxMinor + minMinor) / 2);
		int maxCentre = ((maxMajor + minMajor) / 2);
		
		boolean pass = imageResult(img, minCentre, maxMajor, minMajor, curlab);
		
		return pass;
		
	}

	public static void threshold(Mat img, int t)
	{
		byte data[] =  new byte[img.rows()*img.cols()*img.channels()];
		img.get(0, 0, data);
		
		System.out.println("Threshold: " + t);
		
		for (int i = 0; i < data.length; i++) {
			int unsigned = (data[i] & 0xff);
			if(unsigned > t)
				data[i] = (byte)0;
			else
				data[i] = (byte)255;
		}
		img.put(0, 0, data);
	}
	
	public static int findThreshold(int[] hist)
	{
		int maxThreshold = hist[0];
		int returnedThresh = 0;
		
		for(int i =0; i < hist.length; i++) {
			if(hist[i] > maxThreshold) {
				maxThreshold = hist[i];
				returnedThresh = i;
			}
		}
		
		return returnedThresh;
	}
	
	public static int [] hist(Mat img)
	{
		int hist[] = new int[256];
		byte data[] = new byte[img.rows()*img.cols()*img.channels()];
		img.get(0, 0, data);
		for(int i=0; i<data.length; i++)
		{
			hist[(data[i]) & 0xff]++;
		}
		return hist;
	}
	
	public static void drawHist(Mat img, int [] hist)
	{
		int max = 0;
		for(int i =0; i<hist.length; i++)
		{
			if(hist[i]>max)
				max = hist[i];
		}
		int scale = max/256;
		for(int i =0; i<hist.length-1; i++)
		{
			Imgproc.line(img, new Point(i+1, img.rows()-(hist[i]/scale)+1), new Point(i+2, img.rows()-(hist[i+1]/scale)+1), new Scalar(0, 0, 255));
		}
	}
	
	public static BufferedImage Mat2BufferedImage(Mat m)
	{
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if(m.channels()>1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte[]b = new byte[bufferSize];
		m.get(0, 0,b);
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}
	
	public static void erode(Mat img)
	{
		byte data[] = new byte[img.rows() * img.cols() *img.channels()];
		img.get(0, 0, data);
		byte copy[]=data.clone();
		
		for(int i = 0; i< data.length; i++) {
			int[] neighbors = {
					i + 1,
					i - 1,
					i + img.cols(),
					i - img.cols(),
					i + img.cols() + 1,
					i + img.cols() - 1,
					i - img.cols() + 1,
					i - img.cols() - 1
			};
			
			try {
				for(int j = 0; j < neighbors.length; j++) {
					if((copy[neighbors[j]] & 0xFF) == 0)
						data[i] = (byte) 0;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				
			}
		}
		img.put(0, 0, data);
	}
	
	public static void fill(Mat img)
	{
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		byte copy[] = data.clone();
		
		for (int i = 0; i < data.length; i++) {
			int[] neighbors = {
					i + 1,
					i - 1,
					i + img.cols(),
					i - img.cols(),
					i + img.cols() + 1,
					i + img.cols() - 1,
					i - img.cols() + 1,
					i - img.cols() - 1
			};
			
			try {
				for(int j = 0; j < neighbors.length; j++) {
					if ((copy[neighbors[j]] & 0xFF) == 255)
						data[i] = (byte) 255;
					}
				} catch (ArrayIndexOutOfBoundsException ex) {
					
				}
			}
			img.put(0, 0, data);
		}
	
	
	public static int[] currentLabel(Mat img) 
	{
		ConnectedComp connectedComp = new ConnectedComp();
		int current = 0;
		int[] curlab = new int[img.rows() *img.cols() * img.channels()];
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		for(int i =0; i < data.length; i++) {
			if((data[i] & 0xFF) == 255 && curlab[i] == 0) {
				current++;
				curlab[i] = current;
				connectedComp.enqueue(i);
				
				while(!connectedComp.isEmpty()) {
					int position = connectedComp.dequeue();
					
					if ((data[position+1] & 0xFF) == 255 && curlab[position+1] == 0) {
						curlab[position+1] = current;
						connectedComp.enqueue(position+1);
					}
					
					if ((data[position-1] & 0xFF) == 255 && curlab[position-1] == 0) {
						curlab[position-1] = current;
						connectedComp.enqueue(position-1);
					}
					
					if ((data[position-img.cols()] & 0xFF) == 255 && curlab[position-img.cols()] == 0) {
						curlab[position-img.cols()] = current;
						connectedComp.enqueue(position-img.cols());
					}
					
					if ((data[position+img.cols()] & 0xFF) == 255 && curlab[position+img.cols()] == 0) {
						curlab[position+img.cols()] = current;
						connectedComp.enqueue(position+img.cols());
					}	
				}
			}
		}
		
		System.out.println("Objects found: " + current);
		return curlab;
	}
	
	public static int countORing(Mat img, int[] curlab)
	{
		Map<Integer, Integer> oRingCount = new HashMap<Integer, Integer>();
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		int currentLabel = 0;
		int oRingLabel = 0;
		int rings = 0;
		
		for(int i = 0; i < curlab.length; i++) {
			if((data[i] & 0xFF) == 255 && curlab[i] == currentLabel) {
				if(oRingCount.get(currentLabel) == null)
					oRingCount.put(currentLabel, 1);
				else
					oRingCount.put(currentLabel, (oRingCount.get(currentLabel)+1));
			}
			else if((data[i] & 0xFF) == 255 && curlab[i] > currentLabel) {
				currentLabel++;
				oRingCount.put(currentLabel, 1);
			}
		}
		for(int j = 1; j <= oRingCount.size(); j++) {
			if(j==1) {
				rings = oRingCount.get(j);
				oRingLabel = j;
			}
			else if(j < 1 && oRingCount.get(j) > rings) {
				rings = oRingCount.get(j);
				oRingLabel = j;
			}
		}
		
		if(oRingCount.size() > 1)
			System.out.println("Ring label = " +oRingLabel+ " Removing background objects");
		
		return oRingLabel;
	}
	
	public static void noiseRemover(Mat img, int oRingLabel, int[]curlab)
	{
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		for(int i =0; i < data.length; i++) {
			if(curlab[i] != oRingLabel)
				data[i] = (byte) 0;
		}
		img.put(0, 0, data);
	}
	
	
	public static int[] ringEdges(Mat img, int[] curlab, int oRingLabel)
	{
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		int[] edges = new int [curlab.length];
		
		for(int i = 0; i < curlab.length; i++) {
			if(curlab[i] == oRingLabel) {
				if(((data[i - img.cols()] & 0xFF) == 0 || (data[i + img.cols()] & 0xFF) == 0) 
						|| ((data[i - 1] & 0xFF) == 0 || (data[i + 1] & 0xFF)== 0)) {
					edges[i] = 1;
				} else {
					edges[i] = 0;
				}
			}
		}
		return edges;
	}
	
}
