package io.github.lemostic.toolsuite.modules.qrcode;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class QRCodeView extends BorderPane {

    private static final int DEFAULT_QR_SIZE = 300;

    private final TextArea inputArea = new TextArea();
    private final ImageView qrImageView = new ImageView();
    private final Slider sizeSlider = new Slider(100, 500, DEFAULT_QR_SIZE);
    private final Label sizeLabel = new Label("300x300");
    private final ColorPicker fgColorPicker = new ColorPicker(Color.BLACK);
    private final ColorPicker bgColorPicker = new ColorPicker(Color.WHITE);
    private final ChoiceBox<String> errorCorrectionChoice = new ChoiceBox<>();
    private final CheckBox compressCheckBox = new CheckBox("压缩内容");
    private final Label compressInfoLabel = new Label();

    private final QRCodeService service = new QRCodeService();

    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public QRCodeView() {
        initializeUI();
        inputArea.setText("https://github.com/lemostic");
        generateQR();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("二维码生成器");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #1a1a2e;");

        HBox topBox = new HBox(titleLabel);
        topBox.setPadding(new Insets(15, 20, 10, 20));
        topBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        setTop(topBox);

        VBox leftPanel = createInputPanel();
        leftPanel.setPrefWidth(380);
        leftPanel.setMinWidth(320);
        leftPanel.setMaxWidth(420);

        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        VBox rightPanel = createPreviewPanel();

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftScroll, rightPanel);
        splitPane.setDividerPosition(0, 0.42);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox centerBox = new VBox(splitPane);
        centerBox.setPadding(new Insets(10, 20, 20, 20));
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        setCenter(centerBox);
    }

    private VBox createInputPanel() {
        Label inputLabel = new Label("内容");
        inputLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));

        inputArea.setPrefRowCount(7);
        inputArea.setWrapText(true);
        inputArea.setPromptText("输入文本、链接或任意内容...");
        inputArea.setStyle("-fx-font-size: 13px; -fx-padding: 10;");

        Label formatLabel = new Label("纠错等级");
        formatLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        formatLabel.setPadding(new Insets(15, 0, 5, 0));

        errorCorrectionChoice.getItems().addAll("L (低 7%)", "M (中 15%)", "Q (较高 25%)", "H (高 30%)");
        errorCorrectionChoice.setValue("M (中 15%)");
        errorCorrectionChoice.setMaxWidth(Double.MAX_VALUE);

        compressCheckBox.setFont(Font.font("System", FontWeight.NORMAL, 13));
        compressCheckBox.selectedProperty().addListener((obs, old, val) -> {
            updateCompressInfo();
            if (val && !inputArea.getText().isBlank()) {
                generateQR();
            } else if (!val && !inputArea.getText().isBlank()) {
                generateQR();
            }
        });

        compressInfoLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        compressInfoLabel.setWrapText(true);

        Label colorLabel = new Label("颜色");
        colorLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        colorLabel.setPadding(new Insets(15, 0, 5, 0));

        Label fgLabel = new Label("前景色:");
        Label bgLabel = new Label("背景色:");

        fgColorPicker.setValue(Color.BLACK);
        bgColorPicker.setValue(Color.WHITE);

        HBox colorBox = new HBox(15,
            new HBox(5, fgLabel, fgColorPicker),
            new HBox(5, bgLabel, bgColorPicker)
        );
        colorBox.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabelTitle = new Label("尺寸");
        sizeLabelTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        sizeLabelTitle.setPadding(new Insets(15, 0, 5, 0));

        sizeSlider.setShowTickLabels(false);
        sizeSlider.setShowTickMarks(false);
        sizeSlider.setMajorTickUnit(100);
        sizeSlider.setBlockIncrement(50);
        sizeSlider.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            sizeLabel.setText(v + "x" + v);
        });

        HBox sliderBox = new HBox(10, sizeSlider, sizeLabel);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        Button btnGenerate = new Button("生成二维码");
        btnGenerate.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;"
        );
        btnGenerate.setMaxWidth(Double.MAX_VALUE);
        btnGenerate.setOnAction(e -> generateQR());

        HBox actionBox = new HBox(10, btnGenerate);
        actionBox.setPadding(new Insets(15, 0, 0, 0));

        VBox panel = new VBox(6,
            inputLabel,
            inputArea,
            formatLabel,
            errorCorrectionChoice,
            compressCheckBox,
            compressInfoLabel,
            colorLabel,
            colorBox,
            sizeLabelTitle,
            sliderBox,
            actionBox
        );
        panel.setPadding(new Insets(15));
        panel.setStyle(
            "-fx-background-color: white; -fx-border-color: #e8e8e8; " +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        return panel;
    }

    private VBox createPreviewPanel() {
        Label previewLabel = new Label("预览");
        previewLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

        qrImageView.setPreserveRatio(true);
        qrImageView.setSmooth(true);

        ScrollPane imageScroll = new ScrollPane(qrImageView);
        imageScroll.setFitToWidth(true);
        imageScroll.setFitToHeight(true);
        imageScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        imageScroll.setPannable(true);

        BorderPane imageContainer = new BorderPane();
        imageContainer.setCenter(imageScroll);
        imageContainer.setMinSize(300, 300);

        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        Button btnSave = new Button("保存图片");
        btnSave.setStyle(
            "-fx-background-color: #2ecc71; -fx-text-fill: white; " +
            "-fx-padding: 8 25; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;"
        );
        btnSave.setOnAction(e -> saveQR());

        Button btnClear = new Button("清空");
        btnClear.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
            "-fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;"
        );
        btnClear.setOnAction(e -> {
            inputArea.clear();
            qrImageView.setImage(null);
        });

        buttonBar.getChildren().addAll(btnSave, btnClear);

        VBox panel = new VBox(10, previewLabel, imageContainer, buttonBar);
        panel.setPadding(new Insets(15));
        panel.setStyle(
            "-fx-background-color: white; -fx-border-color: #e8e8e8; " +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        VBox.setVgrow(imageContainer, Priority.ALWAYS);
        return panel;
    }

    private void updateCompressInfo() {
        if (!compressCheckBox.isSelected()) {
            compressInfoLabel.setText("");
            return;
        }
        String text = inputArea.getText();
        if (text == null || text.isBlank()) {
            compressInfoLabel.setText("输入内容后将自动压缩");
            return;
        }
        try {
            byte[] original = text.getBytes("UTF-8");
            byte[] compressed = compress(original);
            double ratio = (double) compressed.length / original.length * 100;
            compressInfoLabel.setText(String.format("原始: %d bytes → 压缩: %d bytes (%.1f%%)",
                original.length, compressed.length, ratio));
        } catch (Exception e) {
            compressInfoLabel.setText("");
        }
    }

    public static String encodeToBase64(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < data.length) {
            int b0 = data[i++] & 0xFF;
            int b1 = (i < data.length) ? data[i++] & 0xFF : -1;
            int b2 = (i < data.length) ? data[i++] & 0xFF : -1;

            sb.append(BASE64_CHARS.charAt(b0 >> 2));

            if (b1 == -1) {
                sb.append(BASE64_CHARS.charAt((b0 & 0x03) << 4));
                sb.append("==");
            } else {
                sb.append(BASE64_CHARS.charAt(((b0 & 0x03) << 4) | (b1 >> 4)));
                if (b2 == -1) {
                    sb.append(BASE64_CHARS.charAt((b1 & 0x0F) << 2));
                    sb.append("=");
                } else {
                    sb.append(BASE64_CHARS.charAt(((b1 & 0x0F) << 2) | (b2 >> 6)));
                    sb.append(BASE64_CHARS.charAt(b2 & 0x3F));
                }
            }
        }
        return sb.toString();
    }

    private byte[] compress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
            dos.write(data);
        }
        return baos.toByteArray();
    }

    private void generateQR() {
        String text = inputArea.getText();
        if (text == null || text.isBlank()) {
            showAlert("请输入内容");
            return;
        }

        String qrContent = text;

        if (compressCheckBox.isSelected()) {
            try {
                byte[] original = text.getBytes("UTF-8");
                byte[] compressed = compress(original);
                if (compressed.length < original.length) {
                    String prefix = "C1:";
                    qrContent = prefix + encodeToBase64(compressed);
                    updateCompressInfo();
                } else {
                    compressInfoLabel.setText("压缩后未减小，使用原始内容");
                }
            } catch (Exception e) {
                compressInfoLabel.setText("压缩失败: " + e.getMessage());
            }
        }

        int size = (int) sizeSlider.getValue();
        Color fg = fgColorPicker.getValue();
        Color bg = bgColorPicker.getValue();

        String eccValue = errorCorrectionChoice.getValue();
        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel ecc;
        if (eccValue.startsWith("L")) ecc = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;
        else if (eccValue.startsWith("Q")) ecc = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q;
        else if (eccValue.startsWith("H")) ecc = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H;
        else ecc = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M;

        Image qrImage = service.generateQRCode(qrContent, size, fg, bg, ecc);
        qrImageView.setImage(qrImage);
    }

    private void saveQR() {
        Image image = qrImageView.getImage();
        if (image == null) {
            showAlert("请先生成二维码");
            return;
        }

        try {
            File dir = new File(System.getProperty("user.home"), "Desktop");
            if (!dir.exists()) dir = new File(System.getProperty("user.home"));
            File file = new File(dir, "QRCode_" + System.currentTimeMillis() + ".png");

            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bImg.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            ImageIO.write(bImg, "png", file);
            showInfo("已保存: " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("保存失败: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
