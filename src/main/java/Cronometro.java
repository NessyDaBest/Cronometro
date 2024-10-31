//A la hora de aplicar los temas claro y oscuro hace desaparecer a la barra de progreso y no se como evitarlo
//por eso estan comentados

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.*;
import java.util.Properties;

public class Cronometro extends Application {

    private TextField inputField;
    private ProgressBar progressBar;
    private Label tiempoLabel;
    private Button iniciarButton;
    private Button cancelarButton;
    private Button guardarButton;
    private Button cargarButton;
    private int tiempoTotal;
    private int tiempoActual;
    private boolean contando;
    private boolean darkMode = true; // Controla el tema claro/oscuro

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Contador de Tiempo");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label instruccionLabel = new Label("Introduce el tiempo en segundos:");
        inputField = new TextField();
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        //progressBar.setStyle("-fx-accent: #4caf50;"); // Color para asegurar visibilidad
        tiempoLabel = new Label("Tiempo: 0 segundos");
        iniciarButton = new Button("Iniciar");
        cancelarButton = new Button("Cancelar");
        guardarButton = new Button("Guardar Tiempo");
        cargarButton = new Button("Cargar Tiempo");
        cancelarButton.setDisable(true);

        root.getChildren().addAll(instruccionLabel, inputField, progressBar, tiempoLabel, iniciarButton, cancelarButton, guardarButton, cargarButton);

        // Maneja eventos de botones
        iniciarButton.setOnAction(e -> iniciarContador());
        cancelarButton.setOnAction(e -> cancelarContador());
        guardarButton.setOnAction(e -> guardarTiempo(primaryStage));
        cargarButton.setOnAction(e -> cargarTiempo(primaryStage));

        // Tema claro/oscuro
        /*
        ToggleButton themeToggle = new ToggleButton("Modo Oscuro");
        themeToggle.setSelected(true);
        themeToggle.setOnAction(e -> {
            darkMode = !darkMode;
            aplicarTema(root);
            themeToggle.setText(darkMode ? "Modo Oscuro" : "Modo Claro");
        });
        root.getChildren().add(themeToggle);
        aplicarTema(root); // Aplicar tema inicial
         */

        Scene scene = new Scene(root, 300, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void iniciarContador() {
        try {
            tiempoTotal = Integer.parseInt(inputField.getText());
            if (tiempoTotal <= 0) {
                throw new NumberFormatException();
            }

            tiempoActual = 0;
            contando = true;
            progressBar.setProgress(0);
            iniciarButton.setDisable(true);
            cancelarButton.setDisable(false);
            inputField.setDisable(true);

            new Thread(() -> {
                while (contando && tiempoActual < tiempoTotal) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                    tiempoActual++;
                    Platform.runLater(this::actualizarUI);
                }

                if (tiempoActual >= tiempoTotal) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Tiempo completado");
                        alert.setHeaderText(null);
                        alert.setContentText("El tiempo ha finalizado!");
                        alert.showAndWait();
                        reproducirSonidoAlerta(); // Reproduce sonido al finalizar
                        reiniciarUI();
                    });
                }
            }).start();

        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Por favor, introduce un número válido mayor que cero.");
            alert.showAndWait();
        }
    }

    private void cancelarContador() {
        contando = false;
        reiniciarUI();
    }
    private void actualizarUI() {
        double progreso = (double) tiempoActual / tiempoTotal;
        progressBar.setProgress(progreso);
        tiempoLabel.setText("Tiempo: " + formatearTiempo(tiempoActual));
    }

    private void reiniciarUI() {
        iniciarButton.setDisable(false);
        cancelarButton.setDisable(true);
        inputField.setDisable(false);
        progressBar.setProgress(0);
        tiempoLabel.setText("Tiempo: 0 segundos");
    }

    private String formatearTiempo(int segundos) {
        int horas = segundos / 3600;
        int minutos = (segundos % 3600) / 60;
        int seg = segundos % 60;
        return String.format("%02d:%02d:%02d", horas, minutos, seg);
    }

    private void reproducirSonidoAlerta() {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                byte[] buf = new byte[1];
                AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, false);
                try (SourceDataLine sdl = AudioSystem.getSourceDataLine(audioFormat)) {
                    sdl.open(audioFormat);
                    sdl.start();
                    for (int i = 0; i < sampleRate / 10; i++) {
                        double angle = i / (sampleRate / 440.0) * 2.0 * Math.PI; // 440 Hz tono
                        buf[0] = (byte) (Math.sin(angle) * 127);
                        sdl.write(buf, 0, 1);
                    }
                    sdl.drain();
                }
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }
    /*
    private void aplicarTema(VBox root) {
        if (darkMode) {
            root.setStyle("-fx-background-color: #2e2e2e;");
            tiempoLabel.setStyle("-fx-text-fill: #ffffff;");
        } else {
            root.setStyle("-fx-background-color: #ffffff;");
            tiempoLabel.setStyle("-fx-text-fill: #000000;");
        }
    }

     */


    private void guardarTiempo(Stage stage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Tiempo");
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                Properties props = new Properties();
                props.setProperty("tiempoTotal", inputField.getText());
                try (FileOutputStream out = new FileOutputStream(file)) {
                    props.store(out, "Tiempo guardado");
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al guardar");
            alert.setContentText("No se pudo guardar el tiempo.");
            alert.showAndWait();
        }
    }

    private void cargarTiempo(Stage stage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Cargar Tiempo");
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                    inputField.setText(props.getProperty("tiempoTotal"));
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al cargar");
            alert.setContentText("No se pudo cargar el tiempo.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}