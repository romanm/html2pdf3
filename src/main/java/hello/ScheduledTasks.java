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
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
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
	//prodact
//	private static String workDir = "/home/holweb/jura/pdf1/";
	//development
	private static String workDir = "/home/roman/jura/html2pdf/";
	private static String dirName = workDir + "OUT/";
	private static String dirPdfName = workDir+ "PDF/";
//	private String dirTmpName = "/home/roman/jura/html2pdf/tmp/";
	final static Path pathStart = Paths.get(dirName);
	private String autoName = "", h2 = "", h3 = "", h4 = "";
	private Document autoDocument = null;
	Element bodyElAutoDocument = null;
	private	int fileIdx = 0;
	Tidy tidy = getTidy();
	DOMReader domReader = new DOMReader();
	int filesCount;
	DateTime startMillis;
	static PeriodFormatter hmsFormatter = new PeriodFormatterBuilder()
			.appendHours().appendSuffix("h ")
			.appendMinutes().appendSuffix("m ")
			.appendSeconds().appendSuffix("s ")
			.toFormatter();

	@Scheduled(fixedRate = 500000000)
	public void reportCurrentTime() {
		startMillis = new DateTime();
		filesCount = countFiles2(pathStart.toFile());
		logger.debug("Files count " + filesCount + ". The time is now " + dateFormat.format(new Date()));
		try {
//			makeLargeHTML();
			makePdfFromHTML();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void makePdfFromHTML() throws IOException {
		Path pathPdf = Paths.get(dirPdfName);
		logger.debug("Start folder : "+pathPdf);
		Files.walkFileTree(pathPdf, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				final FileVisitResult visitFile = super.visitFile(file, attrs);
				final String fileName = file.toString();
				final String[] splitFileName = fileName.split("\\.");
				final String fileExtention = splitFileName[splitFileName.length - 1];
				if("html".equals(fileExtention)){
					logger.debug(fileName);
					try {
						savePdf(fileName, fileName+".pdf");
						Files.delete(file);
					} catch (com.lowagie.text.DocumentException | IOException e) {
						System.out.println(fileName);
						e.printStackTrace();
					}
				}
				return visitFile;
			}
		});}
	private void makeLargeHTML() throws IOException {
		logger.debug("Start folder : "+pathStart);
		Files.walkFileTree(pathStart, new SimpleFileVisitor<Path>() {
			
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				final FileVisitResult visitFile = super.visitFile(file, attrs);
				fileIdx++;
				logger.debug(fileIdx + "" + "/" + filesCount + procentWorkTime() + file);
				if(fileIdx == filesCount){
					saveHtmlAndPdf(autoDocument, dirPdfName + autoName+ ".html");
					return null;
				}
				final String fileName = file.toString();
				String[] folders = fileName.replace(dirName, "").replace(".html", "").split("\\/");
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
						headElAddElement.addElement("title").addText(humanSpace(autoName));
						bodyElAutoDocument.addElement("h1").addText(humanSpace(autoName));
					}
					if(!h2.equals(folders[1]))
					{
						h2 = folders[1];
						bodyElAutoDocument.addElement("h2").addText(humanSpace(h2));
					}
					if(!h3.equals(folders[2]))
					{
						h3 = folders[2];
						bodyElAutoDocument.addElement("h3").addText(humanSpace(h3));
					}
					if(!h4.equals(folders[3]))
					{
						h4 = folders[3];
						bodyElAutoDocument.addElement("h4").addText(humanSpace(h4));
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

			private String humanSpace(String str) {
				return str.replace("_", " ");
			}

			private void saveHtmlAndPdf(Document document, String htmlOutFileName) {
				Element headEl = (Element) document.selectSingleNode("/html/head");
				addUtf8(headEl);
				writeToFile(document, htmlOutFileName);
				if(true)
					return;
				try {
					savePdf(htmlOutFileName, htmlOutFileName+".pdf");
				} catch (com.lowagie.text.DocumentException | IOException e) {
					e.printStackTrace();
				}
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
					logger.debug(""+document.asXML().length());
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

	void savePdf(String htmlOutFileName, String HTML_TO_PDF) throws com.lowagie.text.DocumentException, IOException {
		String url = new File(htmlOutFileName).toURI().toURL().toString();
		logger.debug(procentWorkTime()+" - start - "+HTML_TO_PDF);
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(url);
		renderer.layout();
		OutputStream os = new FileOutputStream(HTML_TO_PDF);
		renderer.createPDF(os);
		os.close();
		logger.debug(procentWorkTime()+" - end - "+HTML_TO_PDF);
	}

	String procentWorkTime() {
		int procent = fileIdx*100/filesCount;
		String workTime = hmsFormatter.print(new Period(startMillis, new DateTime()));
		String procentSecond = " - html2pdf3 - (" + procent + "%, " + workTime + "s)";
		return procentSecond;
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
