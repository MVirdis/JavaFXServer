import javafx.application.Application;
import javafx.event.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ServerGUI extends Application {

    private TextArea textArea;
    private TextField textField;
    private Button button;
    private Server server;
    private boolean connectionMode;

    public void start(Stage primaryStage) {
        connectionMode = true;

        button = new Button("Start");
        button.setLayoutY(220);
        button.setLayoutX(430);
        button.setOnAction((ActionEvent e) -> {
            if(!connectionMode) {
                server.execute(textField.getText());
                textField.setText("");
            } else {
                connectionMode = false;
                textArea.setText("");
                server = new Server(Integer.parseInt(textField.getText().trim()), this);
                server.start();
                button.setText("Execute");
                textArea.appendText("Server running on port " +
                                    Integer.parseInt(textField.getText().trim())+
                                    System.lineSeparator());
                textField.setText("");
            }
        });

        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setMinWidth(200);
        textArea.setLayoutY(20);
        textArea.setLayoutX(10);
        textArea.appendText("Inserire il numero di porta su cui avviare il server");

        textField = new TextField();
        textField.setLayoutY(220);
        textField.setLayoutX(10);
        textField.setMinWidth(400);

        Group root = new Group(textArea, textField, button);

        Scene scene = new Scene(root, 500, 272);

        primaryStage.setTitle("Server!");
        primaryStage.setScene(scene);
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Width: " + newVal);
        });

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Height: " + newVal);
        });
        primaryStage.show();
    }

    public void log(String message) {
        textArea.appendText(message + System.lineSeparator());
    }

    void exit() {
        System.exit(0);
    }

}
