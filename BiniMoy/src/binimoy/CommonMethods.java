/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binimoy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import java.awt.AWTException;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author User
 */
public class CommonMethods {
    
    
    static byte[] createPacket(byte[]cmd,byte[]data) {
		byte[] pack=null;

		try {
			byte[] separator=new byte[1];
			byte[] initializ=new byte[1];
			byte[] data_length = String.valueOf(data.length).getBytes("UTF8");
			initializ[0]=2;
			separator[0]=4;
			pack=new byte[initializ.length+cmd.length+data_length.length+separator.length+data.length];
			System.arraycopy(initializ, 0, pack, 0, initializ.length);
			System.arraycopy(cmd, 0, pack, initializ.length, cmd.length);
			System.arraycopy(data_length, 0, pack, initializ.length+cmd.length, data_length.length);
			System.arraycopy(separator,0,pack,initializ.length+cmd.length+data_length.length,separator.length);
			System.arraycopy(data,0,pack,initializ.length+cmd.length+data_length.length+separator.length,data.length);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pack;

	}
    
/////////////////////////////
static void getFile(DataInputStream dis,DataOutputStream dos) {
		
    RandomAccessFile rs=null;
    JFrame f;
    JProgressBar jb;

    try {


            long currentPointer=0;
            boolean loop_break=false;
            long fileSize=0;

            jb=new JProgressBar(0,(int) fileSize);    
            jb.setBounds(40,40,160,30);         
            jb.setValue(5);    
            jb.setStringPainted(true);    
            f=new JFrame();
            jb.setSize(250,150);
            f.add(jb);

            f.setSize(500, 100);
            jb.setLayout(new FlowLayout());    
            f.setVisible(true);
            f.setVisible(false);

            while(true) {
                    if(dis.read()==2) {
                            byte[] cmd=new byte[3];
                            dis.read(cmd,0,cmd.length);
                            //System.out.println(Integer.parseInt(new String (cmd)));
                            switch(Integer.parseInt(new String (cmd))) {

                            case 124:
                                    String flName= new String(readStream(dis));

                                    //System.out.println(flName);
                                    //String path="F:\\sharesoft\\"+flName;
                                    ///get file size
                                    if(dis.read()==2) {
                                            byte [] dcmd=new byte[3];
                                            dis.read(dcmd,0,dcmd.length);
                                            if(Integer.parseInt(new String (dcmd))==111) 	fileSize=Long.parseLong(new String(readStream(dis)));
                                            else 	System.out.println("can't detect size");
                                    }
                                    else	System.out.println("can't detect size");
                                    f.setVisible(true);
                                    rs=new RandomAccessFile(new File(flName), "rw");
                                    //System.out.println("file created");
                                    dos.write(createPacket("125".getBytes("UTF8"),String.valueOf(currentPointer).getBytes("UTF8")));
                                    dos.flush();
                                    System.out.println("receving...");
                                    break;

                            case 125:
                                    rs.seek(currentPointer);
                                    byte[] temp_buff=readStream(dis);
                                    rs.write(temp_buff,0 , temp_buff.length);
                                    currentPointer=rs.getFilePointer();

                                    dos.write(createPacket("125".getBytes("UTF8"),String.valueOf(currentPointer).getBytes("UTF8")));
                                    dos.flush();
                                    //System.out.println("recveing  "+(currentPointer*100.0/fileSize)+ " %");

                                    jb.setValue((int)currentPointer);
                                    break;

                            case 126: 
                                    System.out.println("successfull");
                                    //JFrame f=new JFrame();  
                                    f.setVisible(false);
                                JOptionPane.showMessageDialog(f,"File recieved successfully.","Alert",JOptionPane.WARNING_MESSAGE);
                                    loop_break=true;
                                    f.dispose();
                                    return;




                            }
                            if(loop_break) break;
                    }
            }
    } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }finally {

    }


	
}
    
 //////////////////////////////
    
    static byte[] readStream(DataInputStream din) {
		byte[] data_buff=null;
		try {
			String buff_len="";
			int b=0;
			while((b=din.read())!=4) {
				buff_len+=(char)b;
			}
			int len=Integer.parseInt(buff_len);
			data_buff=new byte[len];
			int buffPos=0;
			while(buffPos<len) {
				int canReadSize=din.read(data_buff,buffPos,len-buffPos);
				buffPos+=canReadSize;
			}
		}catch(Exception e) {

		}
		return data_buff;
	}
///////////////////////////////////////////////
    
static	int sendFile(DataInputStream dis,DataOutputStream dos,OutputStream out) throws IOException {
							
    PrintWriter pw=new PrintWriter(out,true);
    while(true) {

        try {
            System.out.println("fileChooser");
            JFileChooser fc=new JFileChooser();

            int result=fc.showOpenDialog(new JFrame());
            if(JFileChooser.APPROVE_OPTION==result){
                File file=fc.getSelectedFile();
                int fileSegment=5555;
                long currentPointer=0;
                long fileSize=file.length();

                if(file.canRead()) {

                    RandomAccessFile rd=new RandomAccessFile(file, "r");
                    dos.write(createPacket("124".getBytes("UTF8"), file.getName().getBytes("UTF8")));
                    dos.flush();
                    dos.write(createPacket("111".getBytes("UTF8"),String.valueOf(fileSize).getBytes("UTF8")));
                    dos.flush();
                    boolean loop_break=false;
                    while(true) {

                        byte[] cmd=new byte[3];
                        if(dis.read()==2) {

                                dis.read(cmd,0,cmd.length);
                                //System.out.println(Integer.parseInt(new String (cmd)));
                                switch(Integer.parseInt(new String(cmd))) {

                                case 125:
                                        //System.out.println("got pntr "+currentPointer+"  "+fileSize);

                                        currentPointer=Long.parseLong(new String(readStream(dis)));
                                        //System.out.println("cnt pntr "+currentPointer+"  "+fileSize);
                                        if(currentPointer!=fileSize) {
                                                int buff_len=(int) ((fileSize-currentPointer) >fileSegment ? fileSegment: fileSize-currentPointer);
                                                byte[] tempbuff=new byte[buff_len];
                                                rd.seek(currentPointer);
                                                rd.read(tempbuff,0,buff_len);
                                                dos.write(createPacket("125".getBytes("UTF8"), tempbuff));
                                                dos.flush();
                                                System.out.println("sending  "+(currentPointer*100.0/fileSize)+ " %");


                                                jb.setValue((int) currentPointer);    
                                                //try{Thread.sleep(1);}catch(Exception e){}

                                        }
                                        else {
                                                loop_break=true;
                                                dos.write(createPacket("126".getBytes("UTF8"), "".getBytes()));
                                                dos.flush();

                                                if(loop_break) {
                                                        System.out.println("send sucess");
                                                        //JFrame f=new JFrame();  
                                                    JOptionPane.showMessageDialog(f,"File sent Successfully.","Alert",JOptionPane.WARNING_MESSAGE);  
                                                    f.setVisible(false);
                                                    return;

                                                }
                                        }
                                        break;
                                }
                            }
                            if(loop_break) {
                                    System.out.println("send sucess");
                                    //JFrame f=new JFrame();  
                                JOptionPane.showMessageDialog(f,"File sent Successfully.","Alert",JOptionPane.WARNING_MESSAGE);  
                                f.setVisible(false);
                                return;

                            }
                    }

                }

            }
        }catch(Exception e) {
        }
    }
    //s.close();
}
///////////////////////////////////////////////////////////////////
    static void viewImg(DataInputStream dIn){
        JLabel jl=new JLabel();
        jl.setIcon(null);
        JFrame frame = new JFrame();
        frame.getContentPane().add(jl);
        frame.setVisible(true); 
        int f=0;
        while(true) {

            try {   
                int length = dIn.readInt();
                byte[] message = null;// read length of incoming message
                if(length>0) {
                    message = new byte[length];
                    dIn.readFully(message, 0, message.length); // read the message
                }
                InputStream in = new ByteArrayInputStream(message);
                BufferedImage bImageFromConvert = ImageIO.read(in);
                //		frame.getContentPane().add(new JLabel(new ImageIcon(bImageFromConvert)));


                jl.setIcon(new ImageIcon(bImageFromConvert));
                jl.revalidate();
                //   jl.repaint();
                //  jl.update(jl.getGraphics());

                frame.pack();
                f=1;
            } catch (IOException ex) {
                Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
//////////////////////////////////////////////////////////////////////
    
    	
    static void send_image(DataOutputStream dOut ) throws UnknownHostException, IOException, AWTException {

        Robot bot = new Robot();
        BufferedImage bimg = bot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed);

        // NOTE: The rest of the code is just a cleaned up version of your code

        // Obtain writer for JPEG format
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();

        // Configure JPEG compression: 70% quality
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.7f);

        // Set your in-memory stream as the output
        jpgWriter.setOutput(outputStream);

        // Write image as JPEG w/configured settings to the in-memory stream
        // (the IIOImage is just an aggregator object, allowing you to associate
        // thumbnails and metadata to the image, it "does" nothing)
        jpgWriter.write(null, new IIOImage(bimg, null, null), jpgWriteParam);

        // Dispose the writer to free resources
        jpgWriter.dispose();

        // Get data for further processing...
        byte[] jpegData = compressed.toByteArray();

        /*InputStream in = new ByteArrayInputStream(jpegData);
        BufferedImage bImageFromConvert = ImageIO.read(in);

        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(bImageFromConvert)));
        frame.setVisible(true); 

        frame.pack(); */

        dOut.writeInt(jpegData.length); // write length of the message
        dOut.write(jpegData);  

    }
	
       
}
