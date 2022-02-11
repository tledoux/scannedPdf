# scannedPdf
Petite expérimentation de détection des PDFs issus de scanner

Ce programme illustre une méthode pour détecter les PDFs issus de scanner par opposition à ceux créés directement de façon numérique.

Ce programme est écrit en Java et utilise la librairie [Apache PDFBox](https://pdfbox.apache.org/)

----
Little experiment to detect scanned vs native pdfs

This program is used to illustrate a way of detecting scanned PDF as opposed to native ones.

It's written in Java using the [Apache PDFBox](https://pdfbox.apache.org/) library.

## Installing locally
* Clone files and cd to directory:  
`git clone https://github.com/tledoux/scannedPdf.git && cd scannedPdf` 
* Build the project:
`mvn clean install`
* Execute the program
`cd target; java -jar scannedPdf-1.0.jar <YOUR_DIR_WITH_PDFS> ; cd ..`


