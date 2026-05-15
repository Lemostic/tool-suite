package io.github.lemostic.toolsuite.modules.qrcode;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.FileOutputStream;

public class QRCodeView extends BorderPane {

    private static final int DEFAULT_QR_SIZE = 400;

    private final TextArea inputArea = new TextArea();
    private final ImageView qrImageView = new ImageView();
    private final Slider sizeSlider = new Slider(100, 600, DEFAULT_QR_SIZE);
    private final Label sizeLabel = new Label("400x400");
    private final ColorPicker fgColorPicker = new ColorPicker(Color.BLACK);
    private final ColorPicker bgColorPicker = new ColorPicker(Color.WHITE);

    private final QRCodeService service = new QRCodeService();
    private String lastGeneratedText;

    public QRCodeView() {
        initializeUI();
        inputArea.setText("https://github.com/lemostic");
        generateQR();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f5f5;");

        VBox leftPanel = createInputPanel();
        leftPanel.setPrefWidth(360);
        leftPanel.setMinWidth(300);

        VBox rightPanel = createPreviewPanel();

        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);

        HBox mainContent = new HBox(20, leftPanel, separator, rightPanel);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(10));

        Label titleLabel = new Label("二维码生成器");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        VBox titleBox = new VBox(titleLabel);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        VBox topBox = new VBox(titleBox, createToolbar());
        topBox.setPadding(new Insets(0, 10, 10, 10));

        setTop(topBox);
        setCenter(mainContent);
    }

    private HBox createToolbar() {
        Button btnGenerate = new Button("生成二维码");
        btnGenerate.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 8 25; " +
            "-fx-background-radius: 5; -fx-cursor: hand;"
        );
        btnGenerate.setOnAction(e -> generateQR());

        Button btnClear = new Button("清空");
        btnClear.setStyle(
            "-fx-background-color: #95a5a6; -fx-text-fill: white; " +
            "-fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        btnClear.setOnAction(e -> {
            inputArea.clear();
            qrImageView.setImage(null);
        });

        Button btnSave = new Button("保存图片");
        btnSave.setStyle(
            "-fx-background-color: #2ecc71; -fx-text-fill: white; " +
            "-fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        btnSave.setOnAction(e -> saveQR());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, btnGenerate, btnClear, spacer, btnSave);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5, 0, 0, 0));
        return toolbar;
    }

    private VBox createInputPanel() {
        Label inputLabel = new Label("输入文本 / URL");
        inputLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

        inputArea.setPrefRowCount(6);
        inputArea.setWrapText(true);
        inputArea.setPromptText("在此输入要生成二维码的文本或URL...");
        inputArea.setStyle(
            "-fx-font-size: 13px; -fx-padding: 10; " +
            "-fx-border-color: #ddd; -fx-border-radius: 5;"
        );

        Label settingsLabel = new Label("个性化设置");
        settingsLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        settingsLabel.setPadding(new Insets(15, 0, 5, 0));

        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(100);
        sizeSlider.setBlockIncrement(50);
        sizeSlider.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            sizeLabel.setText(v + "x" + v);
        });

        Label fgLabel = new Label("前景色:");
        Label bgLabel = new Label("背景色:");
        HBox colorBox = new HBox(15,
            new HBox(8, fgLabel, fgColorPicker),
            new HBox(8, bgLabel, bgColorPicker)
        );
        colorBox.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8,
            inputLabel,
            inputArea,
            settingsLabel,
            new Label("二维码尺寸:"),
            new HBox(10, sizeSlider, sizeLabel),
            colorBox
        );
        panel.setPadding(new Insets(10));
        panel.setStyle(
            "-fx-background-color: white; -fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        return panel;
    }

    private VBox createPreviewPanel() {
        Label previewLabel = new Label("预览");
        previewLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

        qrImageView.setPreserveRatio(true);
        qrImageView.setSmooth(true);

        StackPane imageContainer = new StackPane(qrImageView);
        imageContainer.setMinSize(300, 300);
        imageContainer.setPrefSize(420, 420);
        imageContainer.setStyle(
            "-fx-background-color: white; -fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;"
        );

        VBox panel = new VBox(12, previewLabel, imageContainer);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_CENTER);
        return panel;
    }

    private void generateQR() {
        String text = inputArea.getText();
        if (text == null || text.isBlank()) {
            showAlert("请输入文本或URL");
            return;
        }

        lastGeneratedText = text;
        int size = (int) sizeSlider.getValue();
        Color fg = fgColorPicker.getValue();
        Color bg = bgColorPicker.getValue();

        Image qrImage = service.generateQRCode(text, size, fg, bg);
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
            File file = new File(dir, "QRCode_" + System.currentTimeMillis() + ".png");

            java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(
                (int) image.getWidth(), (int) image.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB
            );

            javafx.scene.image.PixelReader reader = image.getPixelReader();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    bImg.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            javax.imageio.ImageIO.write(bImg, "png", file);
            showInfo("已保存至: " + file.getAbsolutePath());
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
