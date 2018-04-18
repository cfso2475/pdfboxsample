package com.skylark.service.pdfsplitter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument; 
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	//Performance improvement in java 8
    	//https://pdfbox.apache.org/2.0/migration.html#pdf-rendering
    	System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        
        //Loading an existing document
        String filename="/Users/george/eclipse-workspace/pdfboxsample/testfile/image1.pdf";
        File file = new File(filename); 
        int dpi=300;
        
        PDDocument document;
		try 
		{
			document = PDDocument.load(file);
			//TODO:Load Chinese Font if needed or install the related font on Windows/Linux
			//PDType0Font font = PDType0Font.load(document, new File("H:\\skylark\\Code\\WCM\\pdfextracter\\STSong.TTF"));
			System.out.println("PDF loaded"); 
			
			//Print PDF structure
				/*
				COSDocument cosdocument=document.getDocument();
				COSStream stream =cosdocument.createCOSStream();
				System.out.println("PDF raw:\n"+ stream.toTextString());
		        */
			
			//Get total number of pages if PDF
			int numerofpages=document.getNumberOfPages();
			PDFTextStripper pdfStripper = new PDFTextStripper();
			for(int i=0;i<numerofpages;i++)
			{
				//Get Image render from the PDF
				PDFRenderer renderer = new PDFRenderer(document);
				//page number start with 0;
				//BufferedImage image=renderer.renderImage(i);
				BufferedImage image=renderer.renderImageWithDPI(i, dpi);
				//ImageIO.write(image, "PNG", new File("/Users/george/eclipse-workspace/pdfboxsample/testfile/page"+i+"_"+dpi+".png"));
				savePNGImage(new File("/Users/george/eclipse-workspace/pdfboxsample/testfile/page"+i+"_"+dpi+".png"),image,dpi);
				
				//Get Text of the PDF page
				PDPage page=document.getPage(i);
				//set start page, page number here start with 1;
				pdfStripper.setStartPage(i+1);
				//set end page
				pdfStripper.setEndPage(i+1);
				//get text
				String text= pdfStripper.getText(document);
				
				if(!text.trim().isEmpty())
				{
					//use resources to get the image resource. 
					//For Image style pdf, it is same with PDFRenderer. 
					//For Text or Text+Image(mix) style pdf, can get only part (exclude text) content)
					//Here to run only in mixed style pdf file and send to OCR for those images between text.
					System.out.println("Extracted text of page "+i+ "\n"+text);
					PDResources pdResources = page.getResources();
			        for (COSName c : pdResources.getXObjectNames()) 
			        {
			            PDXObject o = pdResources.getXObject(c);
			            if (o instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) 
			            {
			            	String imgfilename="/Users/george/eclipse-workspace/pdfboxsample/testfile/image/" + System.nanoTime() + ".png";
			                File tmpfile = new File(imgfilename);
			            		System.out.println("Extract image "+imgfilename);
			                //ImageIO.write(((org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject)o).getImage(), "png", tmpfile);
			            		savePNGImage(tmpfile,((org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject)o).getImage(),dpi);
			            }
			        }
			        
					//TODO:send Image to OCR to supplement to the text.
			        System.out.println("Send inline Image to OCR");
			        System.out.println("Integrate OCR text to pdf text");
			       
					//TODO:write to text file with integrated text.
					
				}
				else
				{
					//TODO:get the image and send to OCR
					System.out.println("No Text can be extracted. Get text from OCR");
					//TODO: write to text file with result
				}	
		    }
			
			//Closing the document
		    document.close();
			
		} 
		catch (InvalidPasswordException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
    }
    
    private static void savePNGImage(File output, BufferedImage image, int dpi)
    {
    		output.delete();
        final String formatName = "png";
        ImageOutputStream stream = null;
        
        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) 
        {
           ImageWriter writer = iw.next();
           ImageWriteParam writeParam = writer.getDefaultWriteParam();
           ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
           IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
           if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) 
           {
              continue;
           }
           
           try 
           {
        	   		setDPI(metadata,dpi);
        	   		stream = ImageIO.createImageOutputStream(output);
				writer.setOutput(stream);
        	   		writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
           } 
           catch (Exception e) 
           {
				// TODO Auto-generated catch block
				e.printStackTrace();
           }
           finally 
           {
              try 
              {
				stream.close();
              } 
              catch (IOException e) 
              {
				// TODO Auto-generated catch block
				e.printStackTrace();
              }
           }
           break;
        }
     }

     private static void setDPI(IIOMetadata metadata,int dpi) throws IIOInvalidTreeException 
     {
    	 	double INCH_2_CM = 2.54d;
        // for PMG, it's dots per millimeter
        double dotsPerMilli = 1.0 * dpi / 10 / INCH_2_CM;

        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(dotsPerMilli));

        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);

        metadata.mergeTree("javax_imageio_1.0", root);
     }
}
