package hello;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public class ScheduledTasks {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private static String dirName = "/home/roman/jura/html2pdf/OUT/";
	private String dirPdfName = "/home/roman/jura/html2pdf/PDF/";
//	private String dirTmpName = "/home/roman/jura/html2pdf/tmp/";
	final static Path pathStart = Paths.get(dirName);
	private String autoName = "", h2 = "", h3 = "", h4 = "";
	private Document autoDocument = null;
	Element bodyElAutoDocument = null;
	private	int fileIdx = 0;
	Tidy tidy = getTidy();
	DOMReader domReader = new DOMReader();
	int filesCount;
	long startMillis;

	@Scheduled(fixedRate = 5000000)
	public void reportCurrentTime() {
		startMillis = Calendar.getInstance().getTimeInMillis();
		filesCount = countFiles2(pathStart.toFile());
		logger.debug("Files count "
				+ filesCount
				+ ". The time is now " + dateFormat.format(new Date()));
		try {
			scanFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void scanFiles() throws IOException {
		logger.debug("Start folder : "+pathStart);
		Files.walkFileTree(pathStart, new SimpleFileVisitor<Path>() {
			
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				final FileVisitResult visitFile = super.visitFile(file, attrs);
				fileIdx++;
				int procent = fileIdx*100/filesCount;
				long sec = (Calendar.getInstance().getTimeInMillis() - startMillis)/1000;
				logger.debug(fileIdx + ""
						+ "/"
						+ filesCount
						+ "("
						+ procent
						+ "%, "
						+ sec
						+ "s)" + file);
				if(fileIdx == filesCount){
					saveHtmlAndPdf(autoDocument, dirPdfName + autoName+ ".html");
					return null;
				}
				final String fileName = file.toString();
				String[] folders = fileName.replace(dirName, "").replace(".html", "").split("\\/");
				logger.debug(""+folders);
				final String[] splitFileName = fileName.split("\\.");
				final String fileExtention = splitFileName[splitFileName.length - 1];
				if("html".equals(fileExtention)){
					if(!autoName.equals(folders[0]))
					{
						if(autoDocument != null)
						{
							saveHtmlAndPdf(autoDocument, dirPdfName + autoName+ ".html");
						}
						autoDocument = DocumentHelper.createDocument();
						Element htmElAutoDocument = autoDocument.addElement("html");
						Element headElAddElement = htmElAutoDocument.addElement("head");
						addUtf8(headElAddElement);
						bodyElAutoDocument = htmElAutoDocument.addElement("body");
						autoName = folders[0];
						//Заголовок документа = ім'я машини
						//Document head = vehicle name
						String autoNameHuman = autoName.replace("_", " ");
						headElAddElement.addElement("title").addText(autoNameHuman);
						bodyElAutoDocument.addElement("h1").addText(autoNameHuman);
					}
					if(!h2.equals(folders[1]))
					{
						h2 = folders[1];
						bodyElAutoDocument.addElement("h2").addText(h2);
					}
					if(!h3.equals(folders[2]))
					{
						h3 = folders[2];
						bodyElAutoDocument.addElement("h3").addText(h3);
					}
					if(!h4.equals(folders[3]))
					{
						h4 = folders[3];
						bodyElAutoDocument.addElement("h4").addText(h4);
					}
					Document document = html2xhtml(file.toFile());
					document.selectSingleNode("/html/body//p[a/@class='print-page-button']").detach();
					document.selectSingleNode("/html/body/div/div[@class='back-to-top']").detach();
					for (Element el : (List<Element>) document.selectNodes("/html/body/div//p/span[contains(text(),'Fig. Fig.')]")) {
						el.setText(el.getText().replace("Fig. Fig.", "Fig. "));
					}
					Node bodyDivEl = document.selectSingleNode("/html/body/div").detach();
					bodyElAutoDocument.add(bodyDivEl);
//					document.selectSingleNode("/html/head/title").detach();
//					saveHtml(document, dirTmpName +"test"+ fileIdx+ ".html");
				}
				return visitFile;
			}

			private void saveHtmlAndPdf(Document document, String htmlOutFileName) {
				System.out.print(htmlOutFileName);
				Element headEl = (Element) document.selectSingleNode("/html/head");
				addUtf8(headEl);
				writeToFile(document, htmlOutFileName);
				try {
					savePdf(htmlOutFileName, htmlOutFileName+".pdf");
				} catch (com.lowagie.text.DocumentException | IOException e) {
					e.printStackTrace();
				}
			}

			private void savePdf(String htmlOutFileName, String HTML_TO_PDF) throws com.lowagie.text.DocumentException, IOException {
				String File_To_Convert = htmlOutFileName;
				String url = new File(File_To_Convert).toURI().toURL().toString();
				System.out.println(" 44 "+url);
				OutputStream os = new FileOutputStream(HTML_TO_PDF);
				ITextRenderer iTextRenderer2 = new ITextRenderer();
				ITextRenderer iTextRenderer = iTextRenderer2;
				ITextRenderer renderer = iTextRenderer;
				renderer.setDocument(url);
				renderer.layout();
				System.out.println(" 49 ");
				renderer.createPDF(os);
				System.out.println(" 51 ");
				os.close();
			}

			private void addUtf8(Element headEl) {
				headEl.addElement("meta").addAttribute("charset", "utf-8");
			}

			OutputFormat prettyPrintFormat = OutputFormat.createPrettyPrint();
			private void writeToFile(Document document, String htmlOutFileName) {
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(htmlOutFileName);
//					HTMLWriter xmlWriter = new HTMLWriter(fileOutputStream, prettyPrintFormat);
					XMLWriter xmlWriter = new XMLWriter(fileOutputStream, prettyPrintFormat);
					xmlWriter.write(document);
					xmlWriter.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			private Document html2xhtml(File file) {
				try {
					FileInputStream fis = new FileInputStream(file);
					org.w3c.dom.Document html2xhtml = tidy.parseDOM(fis, null);
					Document document = domReader.read(html2xhtml);
					return document;
				}catch (java.io.FileNotFoundException e){
					System.out.println("File not found: " + file);
				}
				return null;
			}

			private void parseDom4j(String fileName) {
				SAXReader reader = new SAXReader();
				File file = new File(fileName);
				try {
					Document read = reader.read(file );
				} catch (DocumentException e) {
					e.printStackTrace();
				}
			}

			private void m1(String htmlInFileName, String htmlOutFileName) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(htmlInFileName);
				}catch (java.io.FileNotFoundException e){
					System.out.println("File not found: " + htmlInFileName);
				}
				org.w3c.dom.Document xmlDoc = tidy.parseDOM(fis, null);
				try {
					tidy.pprint(xmlDoc,new FileOutputStream(htmlOutFileName));
				}catch(Exception e){
					
				}
			}
		});
	}

	private Tidy getTidy() {
		Tidy tidy = new Tidy();
		tidy.setShowWarnings(false);
		tidy.setXmlTags(false);
		tidy.setInputEncoding("UTF-8");
		tidy.setOutputEncoding("UTF-8");
		tidy.setXHTML(true);// 
		tidy.setMakeClean(true);
		tidy.setQuoteNbsp(false);
		return tidy;
	}

	public static int countFiles2(File directory) {
		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				count += countFiles2(file); 
			}else
				count++;
		}
		return count;
	}

}
