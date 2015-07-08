package hello;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.tidy.Tidy;

@Component
public class ScheduledTasks {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private String dirName = "/home/roman/jura/html2pdf/OUT/";
	private	int fileIdx = 0;
	Tidy tidy = getTidy();
	DOMReader domReader = new DOMReader();

	@Scheduled(fixedRate = 5000000)
	public void reportCurrentTime() {
		logger.debug("The time is now " + dateFormat.format(new Date()));
		try {
			scanFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void scanFiles() throws IOException {
		final Path path = Paths.get(dirName);
		logger.debug("Start folder : "+path);
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				final FileVisitResult visitFile = super.visitFile(file, attrs);
				fileIdx++;
				logger.debug(fileIdx + "" + file);
				if(fileIdx>1)
					return null;
				final String fileName = file.toString();
				final String[] splitFileName = fileName.split("\\.");
				final String fileExtention = splitFileName[splitFileName.length - 1];
				if("html".equals(fileExtention)){
					logger.debug(fileIdx + fileName);
					Document document = html2xhtml(file.toFile());
					document.selectSingleNode("/html/body//p[a/@class='print-page-button']").detach();
					document.selectSingleNode("/html/head/title").detach();
					for (Element el : (List<Element>) document.selectNodes("/html/body/div//p/span[contains(text(),'Fig. Fig.')]")) {
						el.setText(el.getText().replace("Fig. Fig.", "Fig. "));
					}
					((Element) document.selectSingleNode("/html/head"))
					.addElement("meta").addAttribute("charset", "utf-8");
					writeToFile(document, "test"+ fileIdx+ ".html");
				}
				return visitFile;
			}

			OutputFormat prettyPrintFormat = OutputFormat.createPrettyPrint();
			private void writeToFile(Document document, String htmlOutFileName) {
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(dirName + htmlOutFileName);
					HTMLWriter xmlWriter = new HTMLWriter(fileOutputStream, prettyPrintFormat);
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


}
