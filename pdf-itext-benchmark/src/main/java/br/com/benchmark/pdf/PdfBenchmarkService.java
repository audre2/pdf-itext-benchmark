package br.com.benchmark.pdf;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

@Service
public class PdfBenchmarkService {

    public void generateTextPdf(Map<String, String> data) {
        String cpf = data.get("cpf");
        String filePath = "output/text-" + cpf + ".pdf";
        
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph("Relatório de Atividades").setBold().setFontSize(16));
            document.add(new Paragraph("Nome: " + data.get("nome")));
            document.add(new Paragraph("CPF: " + cpf));
            document.add(new Paragraph("Data: " + data.get("data")));
            document.add(new Paragraph("Status: " + data.get("status")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fillTemplatePdf(Map<String, String> data) {
        String cpf = data.get("cpf");
        String filePath = "output/template-output-" + cpf + ".pdf";
        try (PdfReader reader = new PdfReader("src/main/resources/template.pdf");
             PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(reader, writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph(data.get("nome")).setFixedPosition(100, 698, 300));
            document.add(new Paragraph(cpf).setFixedPosition(100, 668, 200));
            document.add(new Paragraph(data.get("dataNascimento")).setFixedPosition(180, 638, 150));

            String base64 = new String(Files.readAllBytes(Paths.get("src/main/resources/assinatura-base64.txt")));
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            Image image = new Image(ImageDataFactory.create(imageBytes));
            image.setFixedPosition(40, 510);
            document.add(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateMultiPageWithTable() {
        int randomNumber = new Random().nextInt(999999);
        String filePath = "output/multipage-" + randomNumber + ".pdf";
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Extrato de Transações").setBold().setFontSize(16));
            document.add(new Paragraph(" "));

            int entries = 150;
            float[] columnWidths = {100F, 250F, 100F};
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("Data").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Descrição").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Valor (R$)").setBold()));

            java.time.LocalDate date = java.time.LocalDate.of(2024, 1, 1);
            double total = 0;

            for (int i = 1; i <= entries; i++) {
                String data = date.plusDays(i).toString();
                String descricao = "Serviço " + i;
                double valor = Math.round((50 + Math.random() * 450) * 100.0) / 100.0;
                total += valor;

                table.addCell(data);
                table.addCell(descricao);
                table.addCell(String.format("R$ %.2f", valor).replace(".", ","));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total: R$ " + String.format("%.2f", total).replace(".", ","))
                    .setBold().setFontSize(12));

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

            try (PdfWriter writer = new PdfWriter(outputPath);
                 PdfDocument mergedPdf = new PdfDocument(writer);
                 PdfDocument doc1 = new PdfDocument(new PdfReader(templateToUse));
                 PdfDocument doc2 = new PdfDocument(new PdfReader("src/main/resources/termo.pdf"))) {

                PdfMerger merger = new PdfMerger(mergedPdf);
                merger.merge(doc1, 1, doc1.getNumberOfPages());
                merger.merge(doc2, 1, doc2.getNumberOfPages());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
