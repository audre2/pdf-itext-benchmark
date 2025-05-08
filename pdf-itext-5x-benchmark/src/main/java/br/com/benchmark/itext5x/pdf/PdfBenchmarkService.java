package br.com.benchmark.itext5x.pdf;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfBenchmarkService {

    public void generateTextPdf(Map<String, String> data) {
        String cpf = data.get("cpf");
        String filePath = "output/text-" + cpf + ".pdf";

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();

            document.add(new Paragraph("Relatório de Atividades", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Nome: " + data.get("nome")));
            document.add(new Paragraph("CPF: " + cpf));
            document.add(new Paragraph("Data: " + data.get("data")));
            document.add(new Paragraph("Status: " + data.get("status")));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fillTemplatePdf(Map<String, String> data) {
        String cpf = data.get("cpf");
        String filePath = "output/template-output-" + cpf + ".pdf";

        try {
            PdfReader reader = new PdfReader("src/main/resources/template.pdf");
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(filePath));
            PdfContentByte content = stamper.getOverContent(1);

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            content.beginText();
            content.setFontAndSize(bf, 12);
            content.showTextAligned(Element.ALIGN_LEFT, data.get("nome"), 100, 698, 0);
            content.showTextAligned(Element.ALIGN_LEFT, cpf, 100, 668, 0);
            content.showTextAligned(Element.ALIGN_LEFT, data.get("dataNascimento"), 180, 638, 0);
            content.endText();

            String base64 = new String(Files.readAllBytes(Paths.get("src/main/resources/assinatura-base64.txt")));
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            Image image = Image.getInstance(imageBytes);
            image.setAbsolutePosition(40, 510);
            content.addImage(image);

            stamper.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateMultiPageWithTable() {
        int randomNumber = new Random().nextInt(999999);
        String filePath = "output/multipage-" + randomNumber + ".pdf";

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, fos);

            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

            document.add(new Paragraph("Extrato de Transações", titleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2f, 5f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            table.addCell(new PdfPCell(new Phrase("Data", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Descrição", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Valor (R$)", headerFont)));

            java.time.LocalDate date = java.time.LocalDate.of(2024, 1, 1);
            double total = 0;

            for (int i = 1; i <= 200; i++) {
                String data = date.plusDays(i).toString();
                String descricao = "Serviço " + i;
                double valor = Math.round((50 + Math.random() * 450) * 100.0) / 100.0;
                total += valor;

                PdfPCell dataCell = new PdfPCell(new Phrase(data, cellFont));
                dataCell.setPaddingBottom(10f);
                table.addCell(dataCell);

                PdfPCell descCell = new PdfPCell(new Phrase(descricao, cellFont));
                descCell.setPaddingBottom(10f);
                table.addCell(descCell);

                PdfPCell valorCell = new PdfPCell(new Phrase(String.format("R$ %.2f", valor).replace(".", ","), cellFont));
                valorCell.setPaddingBottom(10f);
                table.addCell(valorCell);
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total: R$ " + String.format("%.2f", total).replace(".", ","), totalFont));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mergeTemplateAndTermoByCpf(String cpf) {
        String inputTemplate = "output/template-output-" + cpf + ".pdf";
        String fallbackTemplate = "src/main/resources/template.pdf";
        String outputPath = "output/merged-" + cpf + ".pdf";

        try {
            String templateToUse = Files.exists(Paths.get(inputTemplate)) ? inputTemplate : fallbackTemplate;

            byte[] templateBytes = Files.readAllBytes(Paths.get(templateToUse));
            byte[] termoBytes = Files.readAllBytes(Paths.get("src/main/resources/termo.pdf"));

            PdfReader reader1 = new PdfReader(new ByteArrayInputStream(templateBytes));
            PdfReader reader2 = new PdfReader(new ByteArrayInputStream(termoBytes));

            Document document = new Document();
            PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputPath));
            document.open();

            for (int i = 1; i <= reader1.getNumberOfPages(); i++) {
                copy.addPage(copy.getImportedPage(reader1, i));
            }
            reader1.close();

            for (int i = 1; i <= reader2.getNumberOfPages(); i++) {
                copy.addPage(copy.getImportedPage(reader2, i));
            }
            reader2.close();

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
